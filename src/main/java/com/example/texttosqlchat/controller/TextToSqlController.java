package com.example.texttosqlchat.controller;

import com.example.texttosqlchat.dto.TextToSqlRequest;
import com.example.texttosqlchat.dto.SqlResponse;
import com.example.texttosqlchat.service.DatabaseExecutionService;
import com.example.texttosqlchat.service.LLMService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sql")
public class TextToSqlController {

    private final LLMService llmService;
    private final DatabaseExecutionService dbService;

    public TextToSqlController(LLMService llmService, DatabaseExecutionService dbService) {
        this.llmService = llmService;
        this.dbService = dbService;
    }

    @PostMapping("/generate-and-execute")
    public ResponseEntity<SqlResponse> generateAndExecuteSql(@RequestBody TextToSqlRequest request) {
        if (request.getNaturalLanguageQuery() == null || request.getNaturalLanguageQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    SqlResponse.builder().status("Error: Query cannot be empty.").build()
            );
        }

        // 1. Generate SQL from LLM (schema is fetched internally by LLMService)
        String generatedSql = llmService.generateSqlQuery(request.getNaturalLanguageQuery());

        if (generatedSql == null || generatedSql.trim().isEmpty()) {
            return ResponseEntity.ok(
                    SqlResponse.builder().status("Error: LLM failed to generate a query.").build()
            );
        }

        // 2. Execute against real DB
        try {
            List<Map<String, Object>> results = dbService.executeQuery(generatedSql);
            return ResponseEntity.ok(
                    SqlResponse.builder()
                            .generatedSql(generatedSql)
                            .queryResult(results)
                            .status("Successfully generated and executed.")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.ok(
                    SqlResponse.builder()
                            .generatedSql(generatedSql)
                            .status("SQL Execution Error: " + e.getMessage())
                            .build()
            );
        }
    }
}