<div align="center">
  <h1>🍔 QuickBite Online Food Delivery Platform</h1>
  <p><i>A scalable, microservices-based food delivery platform connecting customers, restaurants, and delivery agents.</i></p>
</div>

---

## 📖 Overview

**QuickBite** is a comprehensive, end-to-end online food delivery system built with a modern **Event-Driven Microservices Architecture**. It seamlessly integrates various stakeholders in the food delivery ecosystem, providing tailored interfaces and secure operations for **Customers**, **Restaurant Owners**, **Delivery Agents**, and **Platform Administrators**.

The platform is designed to handle high concurrency and provides real-time features like order tracking, instantaneous payment processing, and dynamic delivery assignments.

---

## 🏗️ Architecture & Core Services

The backend is built using **Java 21** and **Spring Boot 3**, orchestrated via **Netflix Eureka** for service discovery and **Spring Cloud Gateway** as the API Gateway. Communication between services is handled synchronously via **OpenFeign** and asynchronously using **Apache Kafka**.

### 🔹 Infrastructure Services
- **API Gateway (`api-gateway`)**: The single entry point for all client requests. Handles routing, CORS, and delegates authentication to the Auth Service.
- **Service Registry (`eureka-server`)**: Netflix Eureka server for centralized service discovery and registration.

### 🔹 Business Microservices
- **Auth Service (`auth-service`)**: Centralized authentication and authorization using JWT and Spring Security. Manages Role-Based Access Control (RBAC).
- **Restaurant Service (`restaurant-service`)**: Manages restaurant onboarding, profiles, and operational status.
- **Menu Service (`menu-service`)**: Handles the creation and management of food items, categories, and real-time stock availability.
- **Cart Service (`cart-service`)**: Manages user shopping carts, calculating totals, taxes, and handling restaurant-specific cart validation.
- **Order Service (`order-service`)**: The core orchestrator for the order lifecycle. Coordinates with Payment and Delivery services.
- **Payment Service (`payment-service`)**: Integrates with Razorpay for secure transaction processing and handles asynchronous payment webhooks.
- **Delivery Service (`delivery-service`)**: Manages delivery agents, proximity-based order assignments, and delivery status updates.
- **Tracking Service (`tracking-service`)**: Utilizes WebSockets and Kafka to push real-time, live-location order updates directly to the customer's frontend.
- **Review Service (`review-service`)**: Allows customers to leave ratings and feedback for food items and restaurants.

### 🔹 Frontend
- **QuickBite Frontend (`quickbite-frontend`)**: A responsive web application providing the user interface for all platform interactions.

---

## 🚀 Tech Stack

### Backend
- **Java 21 & Spring Boot 3** (REST APIs, Microservices)
- **Spring Cloud** (Gateway, Netflix Eureka, OpenFeign)
- **Spring Security & JWT** (Authentication & RBAC)
- **Apache Kafka** (Event-Driven Messaging)
- **Spring WebSockets** (Real-time updates)

### Database
- **PostgreSQL / MySQL** (Relational Data Persistence via Spring Data JPA)
- **Redis** (Optional caching / cart session storage)

### External Integrations
- **Razorpay** (Payment Gateway)
- **OAuth2** (Social Login capabilities)

---

## ⚙️ Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- Node.js (for the frontend)
- Apache Kafka & Zookeeper (running locally or via Docker)
- MySQL / PostgreSQL Server

### Running the Services Locally

1. **Start Kafka and Zookeeper**: Ensure your local message broker is running.
2. **Start Infrastructure Services First**:
   - Run `eureka-server` (Typically runs on port `8761`)
   - Run `api-gateway` (Typically runs on port `8080`)
3. **Start Business Services**:
   - Run the remaining Spring Boot services (`auth-service`, `order-service`, etc.) using your IDE or via `mvn spring-boot:run`.
4. **Start the Frontend**:
   - Navigate to the `quickbite-frontend` directory.
   - Run `npm install` followed by `npm run dev` or `npm start`.

*(Note: Make sure to configure your `.env` or `application.yml` files with your specific database credentials and Razorpay API keys).*

---

## 🛡️ Security

- All API requests (except public endpoints like login/register) pass through the **API Gateway** where the JWT token is validated.
- **Role-Based Access Control** ensures that Restaurant Owners can only modify their own menus, and Delivery Agents can only update their assigned orders.
- Passwords are encrypted using **BCryptPasswordEncoder**.

