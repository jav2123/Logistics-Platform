# Microservice Communication Contracts

## Purpose

Define the communication mechanisms between all microservices.

---

# Services

| Service | Technology | Responsibility |
|----------|------------|----------------|
| Auth Service | ASP.NET Core | Authentication and Authorization |
| Shipment Service | Scala Play | Shipment Management |
| Analytics Service | Spring Boot | Reporting and Metrics |

---

# Communication Matrix

| From | To | Protocol | Type |
|------|-----|----------|------|
| Client | API Gateway | HTTPS | Sync |
| Gateway | Auth | HTTPS | Sync |
| Gateway | Shipment | HTTPS | Sync |
| Shipment | RabbitMQ | AMQP | Async |
| RabbitMQ | Analytics | AMQP | Async |

---

# Service Responsibilities

## Auth Service

Owns:

- Users
- Roles
- Refresh Tokens

Does NOT know about:

- Shipments
- Drivers

---

## Shipment Service

Owns:

- Shipments
- Drivers
- Shipment Audit

Does NOT own:

- Users
- Roles

Receives:

- JWT Claims

---

## Analytics Service

Consumes events only.

Never modifies Shipment data.

---

# Communication Rules

1. Services never access another service database.

2. Services communicate through:

- HTTP
- RabbitMQ

3. HTTP is used only for request/response operations.

4. Domain events are published through RabbitMQ.

5. Every service owns its own schema/database.

---

# Failure Strategy

HTTP failures

- Retry only when appropriate.
- Timeout after configurable duration.

Message failures

- Retry
- Dead Letter Queue
- Logging