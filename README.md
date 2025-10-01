# SPRING BOOT 3 API REST - JPA/HIBERNATE Project

![](https://img.shields.io/badge/SpringBoot-3-green)
![](https://img.shields.io/badge/Database-H2-blue.svg)

Example of relations between entities of a bookstore (Books - Customer - Cart)
@OneToOne @OneToMany @ManyToOne
Spring Boot 3 - Rest API - JPA/Hibernate - Lombok - H2 - Unit tests

---

### 🌐 Endpoints
- `/api/cart/{customerId}/add` → **POST**
- `/api/cart/{customerId}/checkout` → **GET**

---

### 🧪 Test the Flow

- ADD BOOK
- `curl -X POST "http://localhost:8080/api/cart/1/add?bookTitle=Spring%20Boot%20Basics" -H "Content-Type: application/json"`

- CHECKOUT
- `curl -X GET "http://localhost:8080/api/cart/1/checkout" -H "Accept: application/json"`
