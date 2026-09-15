# Boarding House & Room Rental Management System - Backend API

[![Java](https://img.shields.io/badge/Java-17_LTS-orange.svg?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-green.svg?style=flat&logo=springsecurity)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat&logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Robust RESTful API backend service for the Boarding House & Room Rental Management System built with **Java 17** and **Spring Boot 3.2**. Provides secure role-based authentication, real-time WebSocket communication, automated multi-channel payment gateways, and Google Gemini AI integration.

---

## 🔗 Repository Navigation
- 📦 **Frontend Repository:** [github.com/Truongquocdat150605/quan-ly-phong-tro-frontend](https://github.com/Truongquocdat150605/quan-ly-phong-tro-frontend)
- 🚀 **Live Production Demo:** [quan-ly-phong-tro-frontend-6fx2h31g2.vercel.app](https://quan-ly-phong-tro-frontend-6fx2h31g2.vercel.app)

---

## ✨ Key Features

- **Authentication & Authorization:**
  - Stateless JWT token-based authentication with BCrypt password hashing.
  - Role-Based Access Control (RBAC): `ADMIN`, `LANDLORD`, and `TENANT`.
  - 6-digit OTP password reset workflow via Gmail (Spring Mail) with 15-minute token expiry.
- **Room & Contract Management:**
  - Full CRUD operations for room availability, pricing, utilities, and amenities.
  - Electronic lease contract creation with atomic database transactions (`@Transactional`).
  - Double-booking race condition prevention via `@CacheEvict` room availability cache invalidation.
- **Automated Billing & Online Payments:**
  - Monthly automated utility & rent invoice generation.
  - **Stripe API** & **PayOS API** payment gateway integration for online settlements.
  - Asynchronous Webhook/IPN notification processing with signature verification.
- **Real-Time Communication:**
  - Low-latency live chat and notification engine powered by **WebSocket (STOMP & SockJS)**.
- **AI Virtual Assistant (Gemini RAG):**
  - **Google Gemini AI (`gemini-3.6-flash`)** integration with dynamic Retrieval-Augmented Generation (RAG) context injection.
  - Rule-based local fallback engine for guaranteed 100% response availability.
- **Code Quality & Monitoring:**
  - **SpringDoc OpenAPI 3.0 (Swagger UI)** for interactive API documentation.
  - **PMD Static Code Analysis** for code quality enforcement.
  - **Spring Boot Actuator** for system health metrics monitoring.

---

## 🛠 Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language & Runtime** | Java 17 (LTS) |
| **Framework** | Spring Boot 3.2.0 |
| **Security** | Spring Security, JJWT (io.jsonwebtoken 0.11.5), BCrypt |
| **Persistence Layer** | Spring Data JPA, Hibernate, MySQL Connector/J |
| **Database** | MySQL 8.0 |
| **Real-Time Messaging** | WebSocket, STOMP, SockJS |
| **External Integrations** | Stripe API (v24.0), PayOS API, Google Gemini AI API, Cloudinary, JavaMailSender |
| **API Docs & Monitoring** | SpringDoc OpenAPI 3.0 (Swagger UI), Spring Boot Actuator |
| **Build & Tooling** | Apache Maven, PMD Static Code Analyzer, Lombok |

---

## 📋 Prerequisites

Before running the backend service locally, ensure you have installed:
- **JDK 17** or higher (`java -version`)
- **Apache Maven 3.8+** (`mvn -version` or use included `./mvnw`)
- **MySQL 8.0+** running locally or remotely (`mysql --version`)
- **Git** (`git --version`)

---

## ⚙️ Environment Variables

Set the following environment variables (or configure `application.properties`):

| Variable Name | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8082` | Server port |
| `DB_URL` | `jdbc:mysql://localhost:3306/quan_ly_phong_tro` | MySQL Connection URL |
| `DB_USER` | `root` | MySQL Username |
| `DB_PASS` | `""` | MySQL Password |
| `MAIL_USERNAME` | `[TODO: Your Email]` | Gmail address for sending notifications |
| `MAIL_PASSWORD` | `[TODO: Your App Password]` | Gmail App Password |
| `PAYOS_CLIENT_ID` | `[TODO: PayOS Client ID]` | PayOS Merchant Client ID |
| `PAYOS_API_KEY` | `[TODO: PayOS API Key]` | PayOS API Key |
| `PAYOS_CHECKSUM_KEY` | `[TODO: PayOS Checksum]` | PayOS Signature Checksum Key |
| `STRIPE_SECRET_KEY` | `[TODO: Stripe Secret Key]` | Stripe Secret Key |
| `GEMINI_API_KEY` | `[TODO: Gemini API Key]` | Google Gemini API Key |
| `CLOUDINARY_CLOUD_NAME`| `[TODO: Cloud Name]` | Cloudinary Cloud Name |
| `CLOUDINARY_API_KEY` | `[TODO: API Key]` | Cloudinary API Key |
| `CLOUDINARY_API_SECRET` | `[TODO: API Secret]` | Cloudinary API Secret |

---

## 🚀 Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Truongquocdat150605/quan-ly-phong-tro-backend.git
   cd quan-ly-phong-tro-backend
   ```

2. **Configure Database:**
   Create a MySQL database named `quan_ly_phong_tro`:
   ```sql
   CREATE DATABASE quan_ly_phong_tro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Build the Application:**
   ```bash
   ./mvnw clean package -DskipTests
   ```

4. **Run the Application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend server will start at `http://localhost:8082`.

---

## 📖 API Documentation

Interactive OpenAPI / Swagger UI documentation is automatically generated:

- **Swagger UI:** `http://localhost:8082/swagger-ui.html`
- **OpenAPI JSON Spec:** `http://localhost:8082/v3/api-docs`

---

## 📂 Folder Structure

```
quanliPT/
├── src/
│   ├── main/
│   │   ├── java/com/example/quanliPT/
│   │   │   ├── config/              # Security, WebSocket, PayOS, Cloudinary configs
│   │   │   ├── controller/          # Auth, Room, Contract, Finance, Integration controllers
│   │   │   ├── dto/                 # Auth, Chat, Notification, Contract DTOs
│   │   │   ├── model/               # JPA Entities (User, Room, Contract, Invoice...)
│   │   │   ├── repository/          # Spring Data JPA Repositories
│   │   │   ├── security/            # JwtUtils, JwtFilter, UserDetailsServiceImpl
│   │   │   └── service/             # Business Logic (Contract, Auth, Billing, Gemini...)
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🖼 Screenshots / Demo

[TODO: Add API Swagger & Architecture Screenshots Here]

---

## 👤 Author & Contact

**Truong Quoc Dat**  
- **Email:** hungma668@gmail.com  
- **GitHub:** [github.com/Truongquocdat150605](https://github.com/Truongquocdat150605)  
- **Role:** Java Developer Intern
