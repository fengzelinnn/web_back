package com.example.web_back.controller;

import com.example.web_back.SearchEngine;
import com.example.web_back.SystemStatusService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchEngine searchEngine;
    private final SystemStatusService systemStatusService;

    public SearchController(SearchEngine searchEngine, SystemStatusService systemStatusService) {
        this.searchEngine = searchEngine;
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchEngine.SearchResult>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "offset", defaultValue = "0") int offset)
            throws IOException, ParseException, SQLException {
        List<SearchEngine.SearchResult> results = searchEngine.search(query, offset);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PostMapping("/search")
    public ResponseEntity<List<SearchEngine.SearchResult>> searchPost(
            @RequestBody Map<String, String> body,
            @RequestParam(value = "offset", defaultValue = "0") int offset)
            throws IOException, ParseException, SQLException {
        String query = body.get("query");
        List<SearchEngine.SearchResult> results = searchEngine.search(query, offset);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            String lastUpdateTime = systemStatusService.getLastUpdateTime();
            response.put("lastUpdateTime", lastUpdateTime);

            Map<String, Integer> collegeNewsCount = systemStatusService.getCollegeNewsCount();
            response.put("collegeNewsCount", collegeNewsCount);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
