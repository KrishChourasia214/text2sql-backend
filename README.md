# Dynamic Text-to-SQL Generator

A full-stack application that translates plain English questions into live, executable SQLite queries using a Large Language Model — no SQL knowledge required.

![Java](https://img.shields.io/badge/Java-21-red.svg)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.5.7-brightgreen.svg)
![React](https://img.shields.io/badge/React-18-blue.svg)
![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)
![Hugging Face](https://img.shields.io/badge/HuggingFace-DeepSeek--V3-yellow.svg)

---

## Overview

The user types a question like:

> *"Show me the names of customers who placed orders for products costing more than $50"*

The system reads the live database schema, sends it alongside the question to an LLM, generates valid SQL, executes it, and returns structured results — all in a single request.

---

## How It Works

```
User Question
     │
     ▼
Schema Extraction
(sqlite_master read at runtime — always current)
     │
     ▼
LLM Inference
(DeepSeek-V3 via Hugging Face generates SQL)
     │
     ▼
SQL Execution
(JdbcTemplate runs query against live SQLite DB)
     │
     ▼
JSON Response
```

No SQL is hardcoded. No query templates. Every request generates fresh SQL against the live schema.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.7, Maven |
| Frontend | React 18, Vite, Tailwind CSS |
| AI Model | DeepSeek-V3 via Hugging Face Inference API |
| Database | SQLite (`database.db`) |
| HTTP Client | Spring WebFlux (WebClient) — non-blocking LLM calls |
| Data Access | JdbcTemplate — dynamic SQL execution |

---

## Project Structure

```
texttosqlchat/
├── pom.xml
├── database.db                          # SQLite database (project root)
└── src/main/
    ├── java/com/example/texttosqlchat/
    │   ├── TexttosqlchatApplication.java
    │   ├── controller/
    │   │   └── TextToSqlController.java  # REST endpoint
    │   ├── service/
    │   │   ├── LLMService.java           # Hugging Face API + SQL generation
    │   │   └── DatabaseExecutionService.java # Schema extraction + SQL execution
    │   └── dto/
    │       ├── TextToSqlRequest.java
    │       └── SqlResponse.java
    └── resources/
        └── application.properties
```

---

## Setup and Configuration

### 1. Prerequisites

- Java 21+
- Maven
- A Hugging Face account with an API token → [Get one here](https://huggingface.co/settings/tokens)

### 2. Database

Place `database.db` in the **project root** (same level as `pom.xml`). The file is already included in this repository with seed data across all five tables.

### 3. Configure `application.properties`

Open `src/main/resources/application.properties` and set the following:

```properties
# Hugging Face LLM
llm.api.base-url=https://router.huggingface.co/v1
llm.api.model=deepseek-ai/DeepSeek-V3.2-Exp:novita
llm.api.key=hf_YOUR_TOKEN_HERE

# SQLite
spring.datasource.url=jdbc:sqlite:database.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=none
```

> ⚠️ The base URL must end with `/v1` — the service appends `/chat/completions` to it internally.  
> ⚠️ Never commit your real API key. Add `application.properties` to `.gitignore` or use environment variables.

---

## Running the Application

### Backend

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run
```

Server starts at `http://localhost:8080`

> On Windows use `mvnw.cmd` instead of `./mvnw`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts at `http://localhost:5173`

---

## API Reference

### Endpoint

```
POST /api/v1/sql/generate-and-execute
```

### Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "naturalLanguageQuery": "Show me all customers from the USA"
}
```

### Response

```json
{
  "generatedSql": "SELECT * FROM customers WHERE country = 'USA';",
  "queryResult": [
    { "id": 1, "name": "Alice Johnson", "country": "USA", "signup_date": "2024-01-05" }
  ],
  "status": "Successfully generated and executed."
}
```

### Example curl

```bash
curl -X POST http://localhost:8080/api/v1/sql/generate-and-execute \
  -H "Content-Type: application/json" \
  -d '{"naturalLanguageQuery": "Show me all customers from the USA"}'
```

---

## Database Schema

The included `database.db` models an e-commerce domain:

| Table | Columns |
|---|---|
| `customers` | id, name, country, signup_date |
| `products` | id, name, category, price |
| `orders` | id, customer_id, order_date, total |
| `order_items` | order_id, product_id, quantity, unit_price |
| `payments` | id, order_id, payment_date, amount, method |

The schema is read from `sqlite_master` at runtime on every request — if the database structure changes, the LLM automatically sees the updated schema without any code changes.

---

## Example Questions

```
Show all customers
Products under $100
Total revenue by payment method
Orders placed in 2024
Customers who ordered more than twice
Most expensive product per category
Show me the names of customers who placed orders for products costing more than $50
```

---

## Safety

The `DatabaseExecutionService` blocks any non-SELECT statement before execution:

```
INSERT / UPDATE / DELETE / DROP → rejected immediately
```

Only `SELECT` queries reach the database.

---

## Common Errors

| Error | Cause | Fix |
|---|---|---|
| `Connection refused` to HF API | Wrong `llm.api.base-url` | Must end with `/v1` |
| `400 Bad Request` from LLM | Invalid or expired API key | Update `llm.api.key` |
| `unable to open database file` | `database.db` not in project root | Move it next to `pom.xml` |
| SQL syntax error in response | LLM newline stripping bug | Ensure `replaceAll("\\s+", " ")` fix is in `LLMService` |
| `queryResult: null` | SQL execution failed | Check `status` field for the specific SQL error |

---
