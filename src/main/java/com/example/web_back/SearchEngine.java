package com.example.web_back;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SearchEngine {
    private Directory indexDirectory;
    private Analyzer analyzer;
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;

    public SearchEngine() throws SQLException, IOException {
        indexDirectory = new RAMDirectory();

        // 创建一个 StandardAnalyzer 的实例
        Analyzer standardAnalyzer = new StandardAnalyzer();

        // 创建一个 LimitTokenCountAnalyzer 的实例，限制词条的最大长度为 32766
        analyzer = new LimitTokenCountAnalyzer(standardAnalyzer, 32766); // 修改这里，使用自定义的分析器

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
            booleanQuery.add(new TermQuery(new Term("content", token)), BooleanClause.Occur.SHOULD); // 修改这里，将 "tokens" 更改为 "content"
        }

        QueryParser queryParser = new QueryParser("content", analyzer);
        Query luceneQuery = queryParser.parse(booleanQuery.build().toString()); // 修改这里，将 booleanQuery 添加到解析器中

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
