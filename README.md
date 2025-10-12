# ClusEye

**ClusEye** is a system that monitors your Kubernetes cluster while providing security and log management in a single platform.

The application can view pods and container images, perform security scans using Trivy, collect logs with Falco, process them via Kafka, and stream them to the frontend in real-time.  
Critical logs are analyzed locally using **Ollama's `mistral:latest` model**.

---

## Features

- Lists pods and containers in the cluster and monitors their status.  
- Scans container images for security vulnerabilities using Trivy.  
- Provides real-time log and alert management.  
- Analyzes critical logs from Falco using a **local AI model (`mistral:latest`)**.  
- Filters and presents alert and log data for analysis.

---

## Technologies Used

- **Backend**: Spring Boot, Java  
- **Frontend**: React  
- **Databases**: MongoDB, Redis  
- **Container and Cluster Management**: Kubernetes, Minikube (local)  
- **Image Security Scan**: Trivy  
- **Log and Alert Management**: Falco, Kafka, WebSocket / SSE  
- **AI Analysis**: Local Ollama `mistral:latest` model  

---

## Environment Variables

The backend requires several environment variables to function correctly:

- **KUBERNETES_CLIENT_CONFIG** – Path to the kubeconfig file used to connect to your Kubernetes cluster. Each user must set this path according to their local system.  
- **MONGODB_HOST** – Hostname or IP of the MongoDB server.  
- **MONGODB_PORT** – Port on which MongoDB is running.  
- **MONGODB_DATABASE** – Database name for storing logs and alerts.  
- **REDIS_HOST** – Hostname or IP of the Redis server for caching.  
- **REDIS_PORT** – Port on which Redis is running.  
- **KAFKA_HOST** – Hostname or IP of the Kafka broker.  
- **KAFKA_BS_PORT** – Port of the Kafka broker.  
- **OLLAMA_URL** – URL of the local Ollama server used for AI analysis.

> **Note:** All paths and ports should be configured according to the user's local setup.



## Installation and Setup

Follow these steps to run ClusEye locally:

### 1. Kubernetes (Minikube)
1. Install Minikube: [https://minikube.sigs.k8s.io/docs/start/](https://minikube.sigs.k8s.io/docs/start/)  
2. Start a local cluster:  
   ```bash
   minikube start
   
3. Set the environment variable `KUBERNETES_CLIENT_CONFIG` to your kubeconfig path.

### 2. MongoDB

1. Install MongoDB: [https://www.mongodb.com/docs/manual/installation/](https://www.mongodb.com/docs/manual/installation/)
2. Start MongoDB locally.
3. Configure the following environment variables:

   ```
   MONGODB_HOST
   MONGODB_PORT
   MONGODB_DATABASE
   ```

### 3. Redis

1. Install Redis: [https://redis.io/](https://redis.io/)
2. Start Redis locally.
3. Configure the following environment variables:

   ```
   REDIS_HOST
   REDIS_PORT
   ```

### 4. Kafka

1. Install Kafka: [https://kafka.apache.org/quickstart](https://kafka.apache.org/quickstart)
2. Start a local Kafka broker.
3. Configure the following environment variables:

   ```
   KAFKA_HOST
   KAFKA_BS_PORT
   ```

### 5. Trivy

1. Install Trivy for image vulnerability scanning: [https://github.com/aquasecurity/trivy/releases/](https://github.com/aquasecurity/trivy/releases/)

### 6. Falco

1. Install Falco with docker for log collection : [https://falco.org/docs/getting-started/falco-docker-quickstart/](https://falco.org/docs/getting-started/falco-docker-quickstart/)
2. Start Falco locally.

### 7. Ollama

1. Install Ollama locally: [https://ollama.com/library/mistral)
2. Download and use the `mistral:latest` model.
3. Set the `OLLAMA_URL` environment variable to point to your local Ollama server.

### 8. Backend (Spring Boot)

1. Build and run the backend:

   ```bash
   ./mvnw spring-boot:run
   ```

### 9. Frontend (React)

1. Navigate to the frontend folder and install dependencies:

   ```bash
   npm install
   npm start
   ```
2. The frontend will be available at `http://localhost:3001`.

---

## Development Status

This project is currently running locally and under active development.
New features and improvements are continuously being added.

