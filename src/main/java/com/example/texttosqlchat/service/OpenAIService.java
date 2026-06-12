package com.example.texttosqlchat.service;

import com.example.texttosqlchat.dto.TextToSqlRequest;
import com.example.texttosqlchat.dto.SqlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private final WebClient webClient;
    private final String llmModel;

    private static final String CHAT_COMPLETION_PATH = "/chat/completions";

    public OpenAIService(
            WebClient.Builder webClientBuilder,
            @Value("${llm.api.base-url}") String baseUrl,
            @Value("${llm.api.model}") String model,
            @Value("${llm.api.key}") String apiKey) {

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.llmModel = model;
    }

    public SqlResponse convertTextToSql(TextToSqlRequest request, String schema) {
        try {
            String prompt = createLlmPrompt(request.getNaturalLanguageQuery(), schema);

            Map<String, Object> payload = Map.of(
                    "model", llmModel,
                    "messages", createMessages(prompt),
                    "max_tokens", 200,
                    "temperature", 0.0
            );

            Map responseMap = webClient.post()
                    .uri(CHAT_COMPLETION_PATH)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String generatedSql = extractSqlFromLlmResponse(responseMap);

            if (generatedSql == null || generatedSql.trim().isEmpty()) {
                return SqlResponse.builder()
                        .status("Error: LLM returned an empty or invalid SQL statement.")
                        .build();
            }

            return SqlResponse.builder()
                    .generatedSql(generatedSql.trim())
                    .status("Success")
                    .build();

        } catch (Exception e) {
            System.err.println("LLM API Call Error: " + e.getMessage());
            return SqlResponse.builder()
                    .status("LLM API Error: " + e.getMessage())
                    .build();
        }
    }

    private String createLlmPrompt(String userQuery, String schema) {
        return String.format("""
            You are an expert SQL translator.
            You must ONLY return a single, valid SQL query based on the user's request and the provided schema.
            DO NOT include any explanations, formatting, markdown tags (like ```sql), or extra text.

            DATABASE SCHEMA:
            %s

            USER REQUEST:
            %s

            SQL QUERY:
            """, schema, userQuery);
    }

    private List<Map<String, String>> createMessages(String systemPrompt) {
        return List.of(
                Map.of("role", "user", "content", systemPrompt)
        );
    }

    private String extractSqlFromLlmResponse(Map response) {
        System.out.println("LLM API Response Received: " + response);
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, String> message = (Map<String, String>) choices.get(0).get("message");
                if (message != null) {
                    return message.get("content");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse LLM response: " + e.getMessage());
        }
        return null;
    }
}