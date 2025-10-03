# SPRING BOOT 3 API REST - JPA/HIBERNATE + Kafka Events Project

![](https://img.shields.io/badge/docker-257bd6?style=for-the-badge&logo=docker&logoColor=white)
![](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
![](https://img.shields.io/badge/SpringBoot-3-green)
![](https://img.shields.io/badge/Database-H2-blue.svg)



Example of relations between entities of a bookstore (Books - Customer - Cart)
@OneToOne @OneToMany @ManyToOne
Spring Boot 3 - Rest API - JPA/Hibernate - Lombok - H2 - Unit tests - Kafka/Zookeeper events

---

# Event Flow with Kafka

This architecture demonstrates how microservices communicate through **Kafka topics** in an event-driven workflow:

1. **Bookstore Service**
    - Produces a checkout event to **`checkout-events-topic`** when a customer checks out.

2. **Payment Service**
    - Consumes messages from **`checkout-events-topic`**.
    - Processes the payment and publishes the result (APPROVED or DECLINED) to **`payment-result-topic`**.

3. **Bookstore (Consumer)**
    - Listens to **`payment-result-topic`**.
    - If the payment is **APPROVED**, it produces a stock update event to **`stock-update-topic`**.
    - If the payment is **DECLINED**, no stock update is triggered.

4. **Inventory Service**
    - Consumes events from **`stock-update-topic`**.
    - Updates the stock accordingly in the system.


    +-------------------+
    |   Bookstore API   |
    | (CartController)  |
    +-------------------+
              |
              |  (produce checkout event)
              v
    +-------------------------+
    |  checkout-events-topic  |
    +-------------------------+
              |
              |  (consume)
              v
    +-------------------+
    |  Payment Service  |
    | (Consumer)        |
    +-------------------+
              |
              |  (produce payment result)
              v
    +-------------------------+
    | payment-result-topic    |
    +-------------------------+
              |
              |  (consume)
              v
    +---------------------------+
    | Bookstore Service (Consumer)
    +---------------------------+
              |
              |  (produce stock update event)
              v
    +-------------------------+
    |  stock-update-topic     |
    +-------------------------+
              |
              |  (consume)
              v
    +-------------------+
    | Inventory Service |
    +-------------------+

This clarifies the roles:
- **Bookstore API** → producer for checkout.
- **Payment Service** → consumer of checkout, producer of result.
- **Bookstore (Consumer)** → reacts to payment results, produces stock events.
- **Inventory Service** → final consumer, updates stock.

## Event Flow (Mermaid Diagram)

```mermaid
flowchart LR
    A[Bookstore Service\n(CartController)] -->|produce| B[(checkout-events-topic)]
    B -->|consume| C[Payment Service]
    C -->|produce| D[(payment-result-topic)]
    D -->|consume| E[Bookstore Service\n(Consumer)]
    E -->|produce| F[(stock-update-topic)]
    F -->|consume| G[Inventory Service]
```

---

### 🌐 Endpoints
- `/api/cart/{customerId}/add` → **POST**
- `/api/cart/{customerId}/checkout` → **GET**

---

## Useful Commands

- Run the app for local development
   ```bash
   docker-compose -f docker-compose.infra.yml up -d
   mvn spring-boot:run
   ```

- Run all the services in containers
   ```bash
   docker-compose -f docker-compose.full.yml up --build
   ```

- Stop all containers, clear the Kafka/Zookeeper data, and restart:
   ```bash
   docker-compose -f docker-compose.infra.yml down -v
   ```

- Verify that the container is running
   ```bash
   docker ps
   ```

- View Kafka logs
   ```bash
   docker logs -f kafka
   ```

---

### 🧪 Test the Flow

- Add a book to cart
   ```bash
   curl -X POST "http://localhost:8080/api/cart/1/add?bookTitle=Spring%20Boot%20Basics" -H "Content-Type: application/json"
   ```
- Execute checkout
   ```bash
   curl -X GET "http://localhost:8080/api/cart/1/checkout" -H "Accept: application/json"`
   ```