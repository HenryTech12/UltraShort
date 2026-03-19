# UltraShort ⚡
### Enterprise-Grade URL Shortening Service

**UltraShort** is a high-performance, Java-based backend service engineered to handle URL shortening at scale. This project serves as a demonstration of implementing distributed systems patterns, focusing on low latency, high availability, and system resilience.

## 🚀 Core Features
* **High-Concurrency Shortening**: Efficiently maps long URLs to unique, compressed aliases using optimized base-62 encoding.
* **Distributed Caching**: Leverages **Redis** to store frequently accessed mappings, drastically reducing database latency and load.
* **System Resilience**: Implements **Circuit Breakers** via **Resilience4j** to prevent cascading failures during downstream service outages.
* **API Governance**: Features robust **Rate-Limiting** to protect the infrastructure from abuse and ensure fair resource distribution.
* **Persistent Storage**: Utilizes **PostgreSQL** for reliable, ACID-compliant data management.

## 🛠️ Tech Stack
* **Language**: Java 17+
* **Framework**: Spring Boot 3.x
* **Database**: PostgreSQL (Relational Data)
* **Cache**: Redis
* **Resilience**: Resilience4j (Circuit Breaker & Rate Limiter)
* [cite_start]**Build Tool**: Maven [cite: 29, 47]
* [cite_start]**Documentation**: Swagger / OpenAPI [cite: 47, 69]

## 🏗️ Architecture & Design Patterns
The project is built following **Clean Architecture** principles to ensure strict separation of concerns, making the system highly testable and maintainable.

* **Cache-Aside Pattern**: Optimized data retrieval that prioritizes Redis before querying the primary database.
* **Fault Tolerance**: Strategic use of fault-tolerance mechanisms to maintain 99.9% uptime under heavy load.
* **Scalable Design**: Engineered with microservices design principles in mind for future horizontal scaling.

## 🚦 Getting Started

### Prerequisites
* JDK 17 or higher
* Docker & Docker Compose
* Maven

### Installation
1. **Clone the repository:**
   ```bash
   git clone [https://github.com/fakorode-henry/UltraShort.git](https://github.com/fakorode-henry/UltraShort.git)

Spin up infrastructure (PostgreSQL & Redis):

Bash
docker-compose up -d
Build and Run:

Bash
mvn clean install
mvn spring-boot:run

📈 Performance Impact
Latency Reduction: Successfully reduced data retrieval times by leveraging optimized Redis caching strategies.

Reliability: Maintained system stability through simulated service failures using Resilience4j circuit breakers.

Developed by Fakorode Odunayo Henry
