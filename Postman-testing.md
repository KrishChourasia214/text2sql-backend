# Postman API Testing Guide — Dynamic Text-to-SQL Generator

**Scope:** Manual API testing of the Spring Boot backend using Postman  
**Base URL:** `http://localhost:8080`  
**Last Updated:** June 2025

---

## ⚙️ Pre-Testing Configuration Checklist

Complete every item in this section **before** sending your first request.

---

### 1. Verify Your `application.properties`

```properties
# Hugging Face API
llm.api.base-url=https://router.huggingface.co/v1
llm.api.model=deepseek-ai/DeepSeek-V3.2-Exp:novita
llm.api.key=hf_YOUR_ACTUAL_TOKEN_HERE

# SQLite Database
spring.datasource.url=jdbc:sqlite:database.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
```

> ⚠️ Note the base URL ends with `/v1` — the service appends `/chat/completions` to this, making the full path `https://router.huggingface.co/v1/chat/completions`  
> ⚠️ `database.db` must be in the project root (same level as `pom.xml`)

---

### 2. Build and Start the Application

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Wait for this in the console before opening Postman:
```
Started TexttosqlchatApplication in X.XXX seconds
```

> On Windows use `mvnw.cmd`

---

### 3. Postman Setup

**Create a Collection** named `Text-to-SQL Generator`

**Set a Collection Variable:**

| Variable | Initial Value | Current Value |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | `http://localhost:8080` |

---

## 🔍 The Endpoint

There is **one endpoint** in this application:

```
POST /api/v1/sql/generate-and-execute
```

| Detail | Value |
|---|---|
| Method | `POST` |
| Path | `/api/v1/sql/generate-and-execute` |
| Request Body | JSON with `naturalLanguageQuery` field |
| Response Format | JSON object |
| Auth Required | No (handled server-side via `application.properties`) |

**Request Body format for every test:**
```json
{
    "naturalLanguageQuery": "your question here"
}
```

**Response format for every test:**
```json
{
    "generatedSql": "SELECT ... ;",
    "queryResult": [ { "column": "value" }, ... ],
    "status": "Successfully generated and executed."
}
```

> In Postman: set method to `POST`, go to `Body → raw → JSON`, paste the request body.

---

## 🧪 Test Cases

---

### TEST 1 — Basic Table Fetch (Smoke Test)

**Purpose:** Confirm the full pipeline works — schema extraction, LLM call, SQL execution, JSON response.

**Request Body:**
```json
{
    "naturalLanguageQuery": "Show all customers"
}
```

**Expected `generatedSql`:**
```sql
SELECT * FROM customers;
```

**Expected `queryResult`:**
```json
[
  { "id": 1, "name": "Alice Johnson", "country": "USA", "signup_date": "2024-01-05" },
  { "id": 2, "name": "Bob Smith", "country": "UK", "signup_date": "2024-02-14" }
]
```

**Pass Criteria:** HTTP 200, `status` is `"Successfully generated and executed."`, `queryResult` is a non-empty array.

---

### TEST 2 — WHERE Clause Filter

**Purpose:** Verify the LLM generates a correct `WHERE` clause.

**Request Body:**
```json
{
    "naturalLanguageQuery": "Show me all customers from the USA"
}
```

**Expected `generatedSql`:**
```sql
SELECT * FROM customers WHERE country = 'USA';
```

**Pass Criteria:** HTTP 200, all rows in `queryResult` have `"country": "USA"`.

---

### TEST 3 — Numeric Comparison

**Purpose:** Test numeric `WHERE` filter on the products table.

**Request Body:**
```json
{
    "naturalLanguageQuery": "Which products cost more than 500 dollars"
}
```

**Expected `generatedSql`:**
```sql
SELECT * FROM products WHERE price > 500;
```

**Pass Criteria:** HTTP 200, every row in `queryResult` has `price > 500`.

---

### TEST 4 — Multi-Table JOIN

**Purpose:** Verify the LLM reasons across multiple tables using the live schema (customers → orders → order_items → products).

**Request Body:**
```json
{
    "naturalLanguageQuery": "Show me the names of customers who have placed orders for products costing more than 50 dollars."
}
```

**Expected `generatedSql`:**
```sql
SELECT DISTINCT c.name FROM customers c JOIN orders o ON c.id = o.customer_id JOIN order_items oi ON o.id = oi.order_id JOIN products p ON oi.product_id = p.id WHERE p.price > 50;
```

**Expected `queryResult`:**
```json
[
  { "name": "Alice Johnson" },
  { "name": "Bob Smith" }
]
```

**Pass Criteria:** HTTP 200, `queryResult` contains customer names only, `status` is `"Successfully generated and executed."`.

> This was the query that previously failed due to the newline-stripping bug. After the fix (`replaceAll("\\s+", " ")`), spaces between keywords are preserved and this query executes correctly.

---

### TEST 5 — Aggregation (GROUP BY + SUM)

**Purpose:** Test aggregate functions and grouping.

**Request Body:**
```json
{
    "naturalLanguageQuery": "What is the total revenue per payment method"
}
```

**Expected `generatedSql`:**
```sql
SELECT method, SUM(amount) AS total_revenue FROM payments GROUP BY method;
```

**Expected `queryResult`:**
```json
[
  { "method": "Credit Card", "total_revenue": 1250.00 },
  { "method": "PayPal", "total_revenue": 430.50 }
]
```

**Pass Criteria:** HTTP 200, one row per distinct payment method with a numeric total.

---

### TEST 6 — COUNT Query

**Purpose:** Verify single-value aggregate results.

**Request Body:**
```json
{
    "naturalLanguageQuery": "How many orders have been placed in total"
}
```

**Expected `generatedSql`:**
```sql
SELECT COUNT(*) AS total_orders FROM orders;
```

**Expected `queryResult`:**
```json
[
  { "total_orders": 15 }
]
```

**Pass Criteria:** HTTP 200, single-element array with a numeric count.

---

### TEST 7 — ORDER BY (Sorting)

**Purpose:** Test descending sort generation.

**Request Body:**
```json
{
    "naturalLanguageQuery": "List all products sorted by price from highest to lowest"
}
```

**Expected `generatedSql`:**
```sql
SELECT * FROM products ORDER BY price DESC;
```

**Pass Criteria:** HTTP 200, `price` values in `queryResult` are in descending order.

---

### TEST 8 — Date Filtering

**Purpose:** Test date-based WHERE clause against `order_date`.

**Request Body:**
```json
{
    "naturalLanguageQuery": "Show me orders placed in 2024"
}
```

**Expected `generatedSql`:**
```sql
SELECT * FROM orders WHERE order_date LIKE '2024%';
```

**Pass Criteria:** HTTP 200, all rows have `order_date` starting with `2024`.

---

### TEST 9 — Edge Case: Destructive Query Attempt

**Purpose:** Confirm the safety guard in `DatabaseExecutionService` blocks non-SELECT statements.

**Request Body:**
```json
{
    "naturalLanguageQuery": "Delete all customers"
}
```

**Expected `generatedSql`:** `DELETE FROM customers;` (LLM will generate this)

**Expected `status`:**
```
"SQL Execution Error: Only SELECT queries are allowed for execution."
```

**Pass Criteria:** HTTP 200, `queryResult` is null, `status` contains the blocked operation message. Data in the DB is untouched.

---

### TEST 10 — Edge Case: Empty Query

**Purpose:** Test input validation in the controller.

**Request Body:**
```json
{
    "naturalLanguageQuery": ""
}
```

**Expected Response:** HTTP 400
```json
{
    "generatedSql": null,
    "queryResult": null,
    "status": "Error: Query cannot be empty."
}
```

**Pass Criteria:** HTTP 400, no LLM call is made, error message is returned immediately.

---

## 🔴 Common Errors & Fixes

| Error | Status | Root Cause | Fix |
|---|---|---|---|
| `Connection refused` | — | App not running | Run `./mvnw spring-boot:run` |
| `400` from LLM, `status` has API error | 200 | Invalid/expired HF API key | Update `llm.api.key` |
| SQL runs but spaces missing in query | 200 | Old `\\n` stripping regex | Use `replaceAll("\\s+", " ")` fix in `LLMService` |
| `queryResult: null` with SQL Execution Error | 200 | LLM generated invalid SQL | Check `generatedSql` field — spaces between keywords missing |
| `unable to open database file` | 500 | `database.db` path wrong | Move `database.db` to project root next to `pom.xml` |
| `No static resource api/...` | 404 | Wrong URL path | Correct path is `/api/v1/sql/generate-and-execute` |
| `405 Method Not Allowed` | 405 | Using GET instead of POST | Switch to POST in Postman method dropdown |

---

## 📋 Test Results Log

| Test # | Prompt (short) | HTTP Status | Generated SQL | queryResult | Pass / Fail | Notes |
|---|---|---|---|---|---|---|
| 1 | Show all customers | | | | | |
| 2 | Customers from USA | | | | | |
| 3 | Products over $500 | | | | | |
| 4 | Customer names, orders > $50 | | | | | |
| 5 | Revenue per payment method | | | | | |
| 6 | Total order count | | | | | |
| 7 | Products sorted by price | | | | | |
| 8 | Orders placed in 2024 | | | | | |
| 9 | Delete all customers | | | | | |
| 10 | Empty query | | | | | |

---

## 📦 Exporting the Postman Collection

1. Right-click the collection → `Export`
2. Choose `Collection v2.1`
3. Save as `text-to-sql-postman-collection.json`
4. Commit under a `/postman` folder in the repo
