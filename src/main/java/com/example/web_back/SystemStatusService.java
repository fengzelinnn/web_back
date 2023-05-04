package com.example.web_back;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class SystemStatusService {

    public String getLastUpdateTime() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "Fzl01620")) {
            String query = "SELECT MAX(time) AS time FROM ser.update_time";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("time");
            } else {
                return null;
            }
        }
    }

    public Map<String, Integer> getCollegeNewsCount() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306", "root", "Fzl01620")) {
            String query = "SELECT name, count as news_count FROM ser.colleges order by count desc;";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            Map<String, Integer> collegeNewsCount = new HashMap<>();
            while (resultSet.next()) {
                String shortName = resultSet.getString("name");
                int newsCount = resultSet.getInt("news_count");
                collegeNewsCount.put(shortName, newsCount);
            }

            return collegeNewsCount;
        }
    }
}
