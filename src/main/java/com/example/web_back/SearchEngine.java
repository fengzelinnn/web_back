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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
    }

    private List<String> tokenizeQuery(String query) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<String> tokens = segmenter.sentenceProcess(query);
        return tokens;
    }

    private void loadInvertedIndexFromDatabase() throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://my-cloud-server:3306", "my-cloud-mysql-database", "my-password")) {
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

    public List<SearchResult> search(String query) throws ParseException, IOException {
        // Tokenize the query
        List<String> queryTokens = tokenizeQuery(query);
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        for (String token : queryTokens) {
            booleanQuery.add(new TermQuery(new Term("tokens", token)), BooleanClause.Occur.MUST);
        }

        QueryParser queryParser = new QueryParser("content", analyzer);
        Query luceneQuery = queryParser.parse(query);

        // Perform the search
        TopDocs topDocs = indexSearcher.search(luceneQuery, 10);

        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);
            String url = doc.get("url");
            String title = doc.get("title");
            String content = doc.get("content");

            results.add(new SearchResult(url, title, content));
        }

        return results;
    }
}
