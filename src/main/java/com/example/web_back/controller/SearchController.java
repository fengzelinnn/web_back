package com.example.web_back.controller;

import com.example.web_back.SearchEngine;
import org.apache.lucene.queryparser.classic.ParseException;
import org.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchEngine searchEngine;

    public SearchController(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchEngine.SearchResult>> search(@RequestParam("query") String query) throws IOException, ParseException, JSONException {
        JSONObject jsonObj = new JSONObject(query);
        String queryString = jsonObj.getString("query");
        List<SearchEngine.SearchResult> results = searchEngine.search(queryString);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("/search")
    public ResponseEntity<List<SearchEngine.SearchResult>> searchPost(@RequestBody String query) throws IOException, ParseException, JSONException {
        JSONObject jsonObj = new JSONObject(query);
        String queryString = jsonObj.getString("query");
        List<SearchEngine.SearchResult> results = searchEngine.search(queryString);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

}
