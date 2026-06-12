package com.example.texttosqlchat.dto;

import lombok.Data;

@Data
public class TextToSqlRequest {

    // The natural language query from the user (e.g., "Show me products under $50 in stock")
    private String naturalLanguageQuery;
}
