# 🏥 Patient Management Microservices

A scalable **microservices-based backend system** built using **Spring Boot**, designed to manage patient data with secure authentication, API gateway routing, and cloud-ready infrastructure.

---

## 🚀 Overview

This project demonstrates a **real-world microservices architecture** with:

* 🔐 Secure authentication using JWT
* 🌐 API Gateway for centralized routing
* 🧩 Independent services (Auth, Patient)
* 🐳 Docker-based containerization
* ☁️ LocalStack for AWS-like local development
* 📡 Kafka integration (event-driven architecture ready)

---

## 🧱 Architecture

```
Client → API Gateway → Auth Service → Database
                    → Patient Service → Database
```

* **API Gateway** → Routes all incoming requests
* **Auth Service** → Handles login & token validation
* **Patient Service** → Manages patient data
* **Kafka (optional)** → Event streaming
* **LocalStack** → Simulates AWS locally

---

## 🛠️ Tech Stack

### Backend

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* Spring Cloud Gateway

### Infrastructure

* Docker
* LocalStack (AWS simulation)
* PostgreSQL
* Kafka

### Tools

* Postman (API testing)
* Maven (build tool)

---

## 📂 Project Structure

```
patient-management/
│
├── api-gateway/
├── auth-service/
├── patient-service/
├── infrastructure/
└── integration-tests/
```

---

## 🔐 Authentication Flow

1. User sends login request
2. Auth Service verifies credentials
3. JWT token is generated
4. Token is used for accessing secured APIs

---

## 📡 API Endpoints

### 🔹 Auth Service

| Method | Endpoint         | Description             |
| ------ | ---------------- | ----------------------- |
| POST   | `/auth/login`    | Login and get JWT token |
| GET    | `/auth/validate` | Validate JWT token      |

---

## ▶️ Running the Project

### 🔹 1. Start Services (Docker)

```bash
docker-compose up
```

---

### 🔹 2. Run LocalStack (optional)

```bash
LOCALSTACK=true mvn clean compile exec:java -Dexec.mainClass="com.pm.stack.LocalStack"
./localstack-deploy.sh
```

---

### 🔹 3. Test APIs (Postman)

#### Login

```
POST http://localhost:4004/auth/login
```

Body:

```json
{
  "email": "testuser@test.com",
  "password": "password123"
}
```

---

#### Validate Token

```
GET http://localhost:4004/auth/validate
```

Header:

```
Authorization: Bearer <token>
```

---

## ⚙️ Features

* ✅ Microservices architecture
* ✅ JWT-based authentication
* ✅ API Gateway routing
* ✅ Dockerized services
* ✅ Cloud-ready infrastructure
* ✅ Local AWS simulation using LocalStack

---

## 🧪 Testing

Integration tests are implemented using:

* JUnit 5
* Rest Assured

---

## 📈 Future Enhancements

* Add service discovery (Eureka)
* Implement centralized logging
* Add CI/CD pipeline
* Deploy on AWS

---

## 👨‍💻 Author

**Sanketh Kottary**

---

## ⭐ Show your support

If you like this project, give it a ⭐ on GitHub!

