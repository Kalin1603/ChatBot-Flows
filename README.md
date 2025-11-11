# Java Dynamic Chatbot Flow Backend

This project is a Java backend service built with **Quarkus** that manages a dynamic chatbot flow based on a JSON configuration. It provides REST endpoints for managing chatbot configurations, a WebSocket endpoint for real-time conversation, and integrates with **Google Gemini API** for AI-powered intent detection.

## Features

* **Dynamic Configuration** – Upload and manage chatbot flows via REST API without restarting the application.
* **Real-time Interaction** – Communicate with the chatbot in real time using WebSockets.
* **AI Intent Detection** – Uses Google Gemini to understand natural language user input.
* **Persistent History** – Stores all conversation entries in a PostgreSQL database.
* **Containerized Deployment** – Ready for Docker environments.
* **Isolated Testing** – Includes a test environment with an in-memory H2 database.

---

## Prerequisites

* JDK 17+ and Gradle 8+
* Docker Desktop
* A running PostgreSQL instance
* [Postman](https://www.postman.com/) for REST API testing
* [PieSocket Tester](https://www.piesocket.com/websocket-tester) or another WebSocket client

---

## Setup and Configuration

### 1. Create the Database

Connect to your PostgreSQL instance and execute the following commands:

```sql
CREATE DATABASE chatbot_db;
CREATE USER chatbot_user WITH PASSWORD 'mysecretpassword';
GRANT ALL PRIVILEGES ON DATABASE chatbot_db TO chatbot_user;
\c chatbot_db
GRANT ALL ON SCHEMA public TO chatbot_user;
```

### 2. Set Up Secrets

Create a `.env` file in the project root directory (ignored by Git) and add your Gemini API key:

```bash
GEMINI_API_KEY=your_actual_api_key_goes_here
```

### 3. Application Properties

The file `src/main/resources/application.properties` is preconfigured to connect to your PostgreSQL database and read the `.env` file. You can override values for your specific setup as needed.

---

## Running and Testing the Application

### Step 1: Run in Development Mode

Start the Quarkus application with live reload:

```bash
./gradlew quarkusDev
```

The service will run on **[http://localhost:8080](http://localhost:8080)**. Note: This is a backend-only service — there is no web interface.

### Step 2: Upload the Flow via Postman

1. Open Postman.
2. Create a **POST** request to `http://localhost:8080/api/config`.
3. In the **Body** tab, choose **raw** → **JSON**.
4. Paste your chatbot flow configuration (e.g., from `flow-example.json`).
5. Send the request. You should receive a **200 OK** response.

### Step 3: Chat with the Bot via WebSocket

1. Open a WebSocket client (e.g., PieSocket Tester).
2. Connect to `ws://localhost:8080/chatbot`.
3. The server sends the initial message defined in your configuration.
4. Send messages like `what is the weather?` to interact with the chatbot.

### Step 4: Verify History in Database

Use a SQL client (e.g., DBeaver) to inspect chat history:

```sql
SELECT * FROM conversationentry;
```

---

## Running with Docker

### Build the Docker Image

Ensure `quarkus.container-image.build=true` is set in `gradle.properties`, then run:

```bash
./gradlew build
```

### Run the Docker Container

Replace placeholder values as needed:

```bash
docker run -i --rm \
  -p 8080:8080 \
  -e GEMINI_API_KEY="your_actual_gemini_key" \
  -e QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://host.docker.internal:5432/chatbot_db" \
  -e QUARKUS_DATASOURCE_USERNAME="chatbot_user" \
  -e QUARKUS_DATASOURCE_PASSWORD="mysecretpassword" \
  your-dockerhub-username/chatbot-backend:1.0
```

---

## Running Automated Tests

The project includes automated unit and integration tests that use an isolated in-memory H2 database. Run them with:

```bash
./gradlew test
```

---

### Summary

This backend service provides a flexible, AI-driven chatbot system that can dynamically adapt to uploaded JSON flows. With Quarkus for performance, Gemini for language understanding, and PostgreSQL for persistence, it is fully equipped for both development and production environments.
