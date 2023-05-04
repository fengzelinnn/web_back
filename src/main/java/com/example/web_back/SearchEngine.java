package com.example.web_back;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.WordDictionary;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchEngine {
    private Directory indexDirectory;
    private Analyzer analyzer;
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;

    public SearchEngine() throws SQLException, IOException {
        indexDirectory = new RAMDirectory();
        analyzer = new StandardAnalyzer();

        // Load the inverted index from the database
        loadInvertedIndexFromDatabase();

        // Create an IndexReader and an IndexSearcher
        indexReader = DirectoryReader.open(indexDirectory);
        indexSearcher = new IndexSearcher(indexReader);
        Path path = Paths.get(new File(getClass().getClassLoader().getResource("dicts.txt").getPath()).getAbsolutePath());
        WordDictionary.getInstance().loadUserDict(path);
    }


    private List<String> tokenizeQuery(String query) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<String> tokens = new ArrayList<>();

        // 分离中文和非中文字符
        Pattern chinesePattern = Pattern.compile("[\\u4e00-\\u9fa5]+");
        Pattern nonChinesePattern = Pattern.compile("[^\\u4e00-\\u9fa5]+");
        Matcher chineseMatcher = chinesePattern.matcher(query);
        Matcher nonChineseMatcher = nonChinesePattern.matcher(query);

        // 处理中文字符
        while (chineseMatcher.find()) {
            Set<String> stopwords = new HashSet<>();
            String chineseText = chineseMatcher.group();
            tokens.addAll(segmenter.sentenceProcess(chineseText));
        }

        // 处理非中文字符
        while (nonChineseMatcher.find()) {
            String nonChineseText = nonChineseMatcher.group();
            String[] nonChineseTokens = nonChineseText.split("\\s+");
            for (String token : nonChineseTokens) {
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
        }

        return tokens;
    }

    private void loadInvertedIndexFromDatabase() throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ser", "root", "Fzl01620")) {
            String query = "SELECT * FROM indexed_documents";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try (IndexWriter indexWriter = new IndexWriter(indexDirectory, config)) {
                while (resultSet.next()) {
                    String url = resultSet.getString("url");
                    String title = resultSet.getString("title");
                    String content = resultSet.getString("content");

                    // Create a Lucene document with the URL, title, and content
                    Document document = new Document();
                    document.add(new StringField("url", url, Field.Store.YES));
                    document.add(new StringField("title", title, Field.Store.YES));
                    document.add(new TextField("content", content, Field.Store.YES));

                    // Add the document to the index
                    indexWriter.addDocument(document);
                }
            }
        }
    }

    public static class SearchResult {
        public String url;
        public String title;
        public String content;

        public SearchResult(String url, String title, String content) {
            this.url = url;
            this.title = title;
            this.content = content;
        }
    }

    public List<SearchResult> search(String query, int offset) throws ParseException, IOException, SQLException {
        List<SearchResult> results = new ArrayList<>();

        if (query.startsWith("school^")) {
            String schoolName = query.substring("school^".length());

            // Perform the search for the specific school
            results = searchBySchool(schoolName, offset);

        } else {
            // Tokenize the query
            List<String> queryTokens = tokenizeQuery(query);
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            for (String token : queryTokens) {
                booleanQuery.add(new TermQuery(new Term("tokens", token)), BooleanClause.Occur.MUST);
            }

            QueryParser queryParser = new QueryParser("content", analyzer);
            Query luceneQuery = queryParser.parse(query);

            // Perform the search
            TopDocs topDocs = indexSearcher.search(luceneQuery, offset + 10);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = indexSearcher.doc(scoreDoc.doc);
                String url = doc.get("url");
                String title = doc.get("title");
                String content = doc.get("content");

                results.add(new SearchResult(url, title, content));
            }
        }

        return results;
    }

    private List<SearchResult> searchBySchool(String schoolName, int offset) throws SQLException, IOException {
        List<SearchResult> results = new ArrayList<>();

        // Get the short_name from the colleges table using the schoolName
        String shortName = getShortNameFromDatabase(schoolName);

        if (shortName != null) {
            Query schoolQuery = new WildcardQuery(new Term("url", "*" + shortName + "*"));
            TopDocs topDocs = indexSearcher.search(schoolQuery, offset + 10);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = indexSearcher.doc(scoreDoc.doc);
                String url = doc.get("url");
                String title = doc.get("title");
                String content = doc.get("content");

                results.add(new SearchResult(url, title, content));
            }
        }

        return results;
    }

    private String getShortNameFromDatabase(String schoolName) throws SQLException {
        String shortName = null;

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ser", "root", "Fzl01620")) {
            String query = "SELECT short_name FROM colleges WHERE name = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, schoolName);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                shortName = resultSet.getString("short_name");
            }
        }

        return shortName;
    }

}