package com.example.texttosqlchat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class LLMService {

    private final WebClient webClient;
    private final String llmModel;
    private final DatabaseExecutionService dbExecutionService;

//     Define the specific API path for chat completions
    private static final String CHAT_COMPLETION_PATH = "/chat/completions";


    public LLMService(
            WebClient.Builder webClientBuilder,
            @Value("${llm.api.base-url}") String baseUrl,
            @Value("${llm.api.model}") String model,
            @Value("${llm.api.key}") String apiKey, DatabaseExecutionService dbExecutionService) {
        this.dbExecutionService = dbExecutionService;

        // Configure WebClient for calling the LLM API
            // NOTE: We rely on the base URL being structured correctly (e.g., https://router.huggingface.co/v1)
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                // Use Bearer token authorization, standard for many APIs
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.llmModel = model;
    }

    /**
     * Generates a SQL query using the Chat Completion API format.
     * @param naturalLanguageQuery The user's question.
     * @return The generated SQL string.
     */
    public String generateSqlQuery(String naturalLanguageQuery) {

        // 1. Define the system instruction (Schema + Rules)
        /* String schema = "CREATE TABLE book (id INTEGER PRIMARY KEY, title TEXT, author TEXT, publication_year INTEGER);" +
                "CREATE TABLE customers (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    name TEXT NOT NULL,\n" +
                "    country TEXT,\n" +
                "    signup_date DATE\n" +
                "); " +
                "CREATE TABLE products (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    name TEXT NOT NULL,\n" +
                "    category TEXT,\n" +
                "    price REAL\n" +
                ");" +
                "CREATE TABLE orders (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    customer_id INTEGER,\n" +
                "    order_date DATE,\n" +
                "    total REAL,\n" +
                "    FOREIGN KEY (customer_id) REFERENCES customers(id)\n" +
                ");\n" +
                "\n" +
                "CREATE TABLE order_items (\n" +
                "    order_id INTEGER,\n" +
                "    product_id INTEGER,\n" +
                "    quantity INTEGER,\n" +
                "    unit_price REAL,\n" +
                "    PRIMARY KEY (order_id, product_id),\n" +
                "    FOREIGN KEY (order_id) REFERENCES orders(id),\n" +
                "    FOREIGN KEY (product_id) REFERENCES products(id)\n" +
                ");\n" +
                "\n" +
                "CREATE TABLE payments (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    order_id INTEGER,\n" +
                "    payment_date DATE,\n" +
                "    amount REAL,\n" +
                "    method TEXT,\n" +
                "    FOREIGN KEY (order_id) REFERENCES orders(id)\n" +
                ");\n";

         */

        String schema = dbExecutionService.extractSchema();
//        System.out.println("Executing Lddl: " + schema);

        String systemInstruction = String.format(
                "You are a helpful assistant that translates natural language questions into SQLite queries. " +
                        "Based on the following schema, write a single SQL query to answer the user request. " +
                        "DO NOT include any explanations, markdown formatting (like ```sql), or extra text. " +
                        "Output ONLY the raw SQL command, ending with a semicolon.\n\n" +
                        "SCHEMA: %s",
                schema
        );

        // 2. Build the messages payload
        List<Message> messages = List.of(
                new Message("system", systemInstruction),
                new Message("user", naturalLanguageQuery)
        );

        // 3. Build the final request object
        var payload = new LLMRequest(llmModel, messages, false);

        try {
            // FIX APPLIED: Use the fixed URI path for the Chat Completion API
            LLMResponse response = webClient.post()
                    .uri(CHAT_COMPLETION_PATH) // Fixed URI
                    .header("Content-Type", "application/json")
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    // Handle non-2xx responses (like 401, 404, 400)
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        // Log the error response body for better debugging
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("LLM API Error: " + clientResponse.statusCode() + " " + errorBody);
                                    return Mono.error(new RuntimeException("LLM API Call Failed: " + clientResponse.statusCode()));
                                });
                    })
                    .bodyToMono(LLMResponse.class)
                    .block();

            // 4. Parse the response structure (choices -> message -> content)
            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                String generatedText = response.choices().get(0).message().content();

                // Clean up the text just in case the LLM adds markdown
                return generatedText.trim().replaceAll("```sql|```", "").replaceAll("\\s+", " ").trim();
            }
            return null;

        } catch (Exception e) {
            System.err.println("Error calling LLM API (Check API Key/URL/Model): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // --- Helper Records for Chat Completion API JSON Serialization/Deserialization ---

    // 1. Request Message Structure (role and content)
    private record Message(String role, String content) {}

    // 2. Main Request Structure
    private record LLMRequest(String model, List<Message> messages, boolean stream) {
        // Constructor that forces stream=false as requested
        LLMRequest(String model, List<Message> messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = false;
        }
    }

    // 3. Response Choice Structure
    private record Choice(Message message) {}

    // 4. Main Response Structure
    private record LLMResponse(List<Choice> choices) {}

//    private final WebClient webClient;
//    private final String llmModel;
//
    // Define the specific API path for chat completions
//    private static final String CHAT_COMPLETION_PATH = "/chat/completions";
//
    // Inject properties from application.properties
//    @Value("${llm.api.base-url}")
//    private String llmApiBaseUrl;
//
//    @Value("${llm.api.model}")
//    private String llmApiModel;
//
//    @Value("${llm.api.key}")
//    private String llmApiKey;
//
//    private final WebClient webClient;
//
//    public LLMService(WebClient.Builder webClientBuilder) {
        // Initialize WebClient with the base URL
//        this.webClient = webClientBuilder
//                .baseUrl(llmApiBaseUrl)
//                .defaultHeader("Authorization", "Bearer " + llmApiKey)
//                .build();
//    }
//
//    public LLMService(
//            WebClient.Builder webClientBuilder,
//            @Value("${llm.api.base-url}") String baseUrl,
//            @Value("${llm.api.model}") String model,
//            @Value("${llm.api.key}") String apiKey) {
//
        // Configure WebClient for calling the LLM API
        // NOTE: We rely on the base URL being structured correctly (e.g., https://router.huggingface.co/v1)
//        this.webClient = webClientBuilder
//                .baseUrl(baseUrl)
//                // Use Bearer token authorization, standard for many APIs
//                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
//                .build();
//        this.llmModel = model;
//    }
//
    /**
     * Generates a SQL query based on a natural language prompt and the database schema.
     * @param naturalLanguageQuery The user's question (e.g., "Find all books by George Orwell").
     * @return The generated SQL string (e.g., "SELECT title, author FROM book WHERE author = 'George Orwell';").
     */
//    public String generateSqlQuery(String naturalLanguageQuery) {
        // Define the schema context for the LLM
//        String schema = "CREATE TABLE book (id INTEGER PRIMARY KEY, title TEXT, author TEXT, publication_year INTEGER);";
//
        // System instruction guides the model to only output the SQL command, nothing else.
//        String prompt = String.format(
//                "Based on the following SQLite schema, write a single SQL query to answer the user request. DO NOT include any explanations, markdown formatting (like ```sql), or extra text. Output ONLY the raw SQL command ending with a semicolon.\n\n" +
//                        "SCHEMA: %s\n\n" +
//                        "USER REQUEST: %s",
//                schema,
//                naturalLanguageQuery
//        );
//
        // Hugging Face inference API payload structure
        // NOTE: This structure may need adjustment based on the specific LLM router
        // or model you are using. This is a common structure for text generation.
//        var payload = new LLMRequest(prompt);
//
//        try {
            // Make the asynchronous API call
//            LLMResponse response = webClient.post()
//                    .uri(CHAT_COMPLETION_PATH)
//                    .header("Content-Type", "application/json")
//                    .body(BodyInserters.fromValue(payload))
//                    .retrieve()
//                    .bodyToMono(LLMResponse.class)
//                    .block(); // Blocking for simplicity in a Controller/Service
//
//            if (response != null && response.generatedText() != null) {
                // Clean up any stray markdown or surrounding text the LLM might include
//                return response.generatedText().trim()
//                        .replaceAll("```sql|```", "")
//                        .trim();
//            }
//            return null;
//        } catch (Exception e) {
//            System.err.println("Error calling LLM API: " + e.getMessage());
//            return null; // Return null on failure
//        }
//    }
//
    // --- Helper Records for JSON Serialization/Deserialization ---

    // Request structure might vary; this is a common basic form
//    private record LLMRequest(String inputs) {}
//
    // Response structure might vary; this is a common basic form
//    private record LLMResponse(String generatedText) {}
}

