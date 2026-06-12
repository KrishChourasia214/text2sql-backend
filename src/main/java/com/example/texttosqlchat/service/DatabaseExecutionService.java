package com.example.texttosqlchat.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DatabaseExecutionService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseExecutionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> executeQuery(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty or null.");
        }

        String lowerCaseQuery = sqlQuery.toLowerCase().trim();
        if (lowerCaseQuery.startsWith("insert") || lowerCaseQuery.startsWith("update")
                || lowerCaseQuery.startsWith("delete") || lowerCaseQuery.startsWith("drop")) {
            throw new UnsupportedOperationException("Only SELECT queries are allowed for execution.");
        }

        System.out.println("Executing LIVE SQL: " + sqlQuery);
        return jdbcTemplate.queryForList(sqlQuery);
    }

    public String extractSchema() {
        String query = "SELECT sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'hibernate_sequence'";

        List<String> ddlStatements = jdbcTemplate.query(query, (rs, rowNum) -> rs.getString("sql"));

        return ddlStatements.stream()
                .filter(ddl -> ddl != null && !ddl.trim().isEmpty())
                .collect(Collectors.joining("\n\n"));
    }
}