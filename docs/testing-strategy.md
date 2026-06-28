# Testing Strategy

## Purpose

This document defines the testing strategy for the Logistics Platform.

The objective is to ensure software quality, reliability, maintainability, and confidence during development while supporting a distributed microservices architecture.

---

# Testing Principles

The testing strategy follows these principles:

* Test business rules before infrastructure.
* Prefer fast tests over slow tests.
* Every bug should result in a regression test.
* Every service must be independently testable.
* Tests must be deterministic.
* Tests should not depend on execution order.
* External dependencies should be isolated whenever possible.

---

# Testing Pyramid

```
                    E2E Tests
                 ----------------
               Integration Tests
            ------------------------
             Contract/API Tests
         -----------------------------
               Unit Tests
```

The majority of tests should be Unit Tests.

---

# Testing Levels

## Unit Tests

### Goal

Validate business logic in complete isolation.

### Scope

* Domain Models
* Services
* Validators
* Value Objects
* Business Rules

### Characteristics

* No database
* No RabbitMQ
* No HTTP
* No external services

### Mocked Dependencies

* Repositories
* Message Publishers
* External APIs

### Examples

* User authentication
* Shipment validation
* Role authorization
* Delivery status transitions

### Expected Coverage

80% or higher for domain logic.

---

## Integration Tests

### Goal

Verify interaction with infrastructure.

### Scope

* Database
* Entity Framework
* Slick
* Spring Data
* RabbitMQ
* Dependency Injection

### Examples

* Repository operations
* Database transactions
* Authentication persistence
* Event publishing
* Event consumption

### Environment

Docker Compose

Each integration test should run against isolated databases.

---

## API Tests

### Goal

Validate REST API behavior.

### Verify

* HTTP Status Codes
* Headers
* Authentication
* Authorization
* Validation
* Response Schemas
* Error Responses

### Example

POST /api/auth/login

Verify:

* 200 OK
* JWT returned
* Refresh Token returned

---

# Contract Tests

## Purpose

Ensure compatibility between communicating services.

### HTTP Contracts

Validate:

* Request schema
* Response schema
* Status codes

### Event Contracts

Validate:

* Event schema
* Required fields
* Version compatibility

Examples

ShipmentCreated

ShipmentAssigned

ShipmentDelivered

ShipmentCancelled

---

# End-to-End Tests

## Goal

Validate complete business workflows.

### Example Scenarios

Scenario 1

Register user

↓

Login

↓

Create shipment

↓

Assign shipment

↓

Deliver shipment

↓

Analytics updated

---

Scenario 2

Invalid login

↓

401 Unauthorized

---

Scenario 3

Unauthorized shipment creation

↓

403 Forbidden

---

# Performance Testing

Performance tests are executed independently from functional tests.

Tool

K6

Objectives

Measure:

* Latency
* Throughput
* Error rate
* Concurrent users

Initial Targets

Average response time:

< 200 ms

95th percentile:

< 500 ms

Error rate:

< 1%

---

# Load Testing

Scenarios

100 users

500 users

1000 users

Stress tests

Progressively increase traffic until failure.

Record:

* CPU usage
* Memory usage
* Database connections
* Request latency

---

# Security Testing

Validate:

* JWT expiration
* Invalid tokens
* Refresh token reuse
* Role authorization
* SQL Injection protection
* XSS protection
* CORS configuration
* Input validation

---

# Database Testing

Verify

* Constraints
* Indexes
* Transactions
* Rollbacks
* Migrations

Performance

Use EXPLAIN ANALYZE for critical queries.

---

# Event Testing

Validate

Event publication

Event consumption

Retry policy

Dead Letter Queue

Idempotency

Duplicate message handling

---

# Test Data

Use isolated datasets.

Never depend on production data.

Each test creates its own data.

Each test cleans up after execution.

---

# Test Naming Convention

Format

MethodName_ShouldExpectedBehavior_WhenCondition

Examples

Authenticate_ShouldReturnJwt_WhenCredentialsAreValid

CreateShipment_ShouldThrowValidationException_WhenOriginIsMissing

AssignShipment_ShouldFail_WhenShipmentAlreadyDelivered

---

# Folder Structure

```
tests/

├── unit/
│
├── integration/
│
├── api/
│
├── contract/
│
├── e2e/
│
├── performance/
│
└── security/
```

Each microservice should have its own test project.

Example

```
auth-service/

    src/

    tests/
        unit/
        integration/
```

---

# Code Coverage

Minimum requirements

| Component      | Coverage |
| -------------- | -------- |
| Domain         | 90%      |
| Services       | 85%      |
| Controllers    | 70%      |
| Infrastructure | 60%      |

Overall project target

80%

Coverage should never replace meaningful tests.

---

# Continuous Integration

Every Pull Request must execute

* Static Analysis
* Unit Tests
* Integration Tests
* API Tests
* Code Coverage
* Docker Build

Deployment is blocked if any mandatory test fails.

---

# Regression Testing

Every reported bug must include

1. Reproducible test
2. Fix implementation
3. Regression test preventing recurrence

No bug should be fixed without adding a corresponding automated test.

---

# Testing Tools

| Service                         | Framework                         |
| ------------------------------- | --------------------------------- |
| Auth Service (.NET)             | xUnit + FluentAssertions + Moq    |
| Shipment Service (Scala)        | ScalaTest + Mockito               |
| Analytics Service (Spring Boot) | JUnit 5 + Mockito                 |
| API Testing                     | Postman / Newman                  |
| Performance                     | K6                                |
| Coverage                        | Coverlet / Scoverage / JaCoCo     |
| Containers                      | Testcontainers (where applicable) |

---

# Quality Gates

A feature is considered complete only if:

* Functional requirements are implemented.
* Unit tests pass.
* Integration tests pass.
* API contracts remain compatible.
* Coverage requirements are met.
* Performance targets are not degraded.
* Documentation is updated.
* Code review has been completed.

---

# Definition of Done

A task is complete when:

* Code follows project conventions.
* Tests are automated.
* No critical static analysis issues exist.
* OpenAPI documentation is updated.
* Database migrations are included if required.
* Logging and error handling follow project standards.
* CI pipeline completes successfully.
* Feature is documented.
