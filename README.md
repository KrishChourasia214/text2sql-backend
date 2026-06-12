# 🤖 Dynamic Text-to-SQL Generator

## 🧠 Overview

This project is a powerful **Text-to-SQL microservice** built with **Spring Boot** that translates **natural language questions** into live, executable **SQLite queries**.  
It bridges the gap between **conversational language** and **relational databases** using a **Large Language Model (LLM)** from the **Hugging Face Inference API**.

Crucially, this application does **not** rely on a fixed, hardcoded schema.  
It dynamically inspects the live SQLite database at runtime, feeding the complete and accurate schema (e.g., `customers`, `orders`, `products`) to the LLM for highly precise query generation.

![Java](https://img.shields.io/badge/Java-17+-red.svg)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Hugging Face](https://img.shields.io/badge/HuggingFace-LLM-yellow.svg)

---

## ✨ Features

- ⚙️ **Dynamic Schema Extraction** — Automatically queries the SQLite database's metadata to extract DDL (`CREATE TABLE ...`) statements for all user tables.
- 🤖 **LLM Integration (Hugging Face TGI)** — Uses a pre-trained LLM (via the Chat Completions API) to convert the user’s question, combined with the database schema, into a raw SQL query string.
- 🧩 **Live SQL Execution** — Executes the generated SQL directly against the connected SQLite database using Spring’s `JdbcTemplate`, ensuring results are always based on the latest data.
- 🧠 **Decoupled Services** — Clear separation of concerns between `LLMService` (LLM communication) and `DatabaseExecutionService` (data retrieval).

---

## 🛠️ Technologies & Prerequisites

| Component | Description |
|------------|-------------|
| ☕ **Java 17+** | Language version used |
| 🧰 **Maven** | Build automation and dependency management |
| 🌱 **Spring Boot 3.x** | Framework for RESTful service creation |
| 🗄️ **SQLite Database** | Lightweight relational database |
| 🤖 **Hugging Face API Token** | Required for accessing the LLM model |

---

## 🚀 Setup and Configuration

### 1. Database Setup

- Ensure your SQLite database file (`database.db`) is correctly configured and contains the necessary tables such as `customers`, `payment`, `orders`, and `products`.
- If you are using a `src/main/resources/data.sql` file, make sure it creates and populates these tables upon application startup.

---

### 2. LLM API Configuration

You must configure your **Hugging Face API** credentials in  
`src/main/resources/application.properties`:

| Property | Example Value | Description |
|-----------|----------------|-------------|
| `llm.api.base-url` | `https://router.huggingface.co` | Base URL for the Chat Completions API |
| `llm.api.model` | `deepseek-ai/DeepSeek-V3.2-Exp:novita` | Text-to-SQL capable model used |
| `llm.api.key` | `hf_YOUR_TOKEN_HERE` | Your actual Hugging Face API token |

> ⚠️ **Important:**
> - If the `llm.api.base-url` is incorrect, the application will fail with a *Connection refused* error.
> - If the API key is invalid or the JSON payload is malformed, you will get a *400 Bad Request* error.

---

## ▶️ Running the Application

### 1. Build the Project

Use Maven to clean and package the application:

```bash
./mvnw clean install

Once the server starts, it will be available at:

http://localhost:8080
```

---
## 🔍 Usage and Testing

The application exposes a single GET endpoint that accepts a natural language prompt as a URL query parameter.

Endpoint:
```bash
GET /api/query?prompt={your_question}
```
You must URL-encode your prompt to handle spaces and special characters.

## 🧪 Example Query

Let’s use the question:
```bash
“Show me the names of customers who have placed orders for products costing more than 50 dollars.”

URL-Encoded Prompt:

Show%20me%20the%20names%20of%20customers%20who%20have%20placed%20orders%20for%20products%20costing%20more%20than%2050%20dollars
```

Full curl Command:
```declarative
curl -X GET 'http://localhost:8080/api/query?prompt=Show%20me%20the%20names%20of%20customers%20who%20have%20placed%20orders%20for%20products%20costing%20more%20than%2050%20dollars'
```
---
## 📜 License

This project is licensed under the MIT License.


## 👨‍💻 Author

Developed by: @codeInfiltr4tor

If you find this project helpful, please ⭐ star the repository to support future development.

