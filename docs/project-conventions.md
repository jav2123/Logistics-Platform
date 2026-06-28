# Project Conventions

---

# Naming

Repositories

EntityNameRepository

Example

ShipmentRepository

---

Services

EntityNameService

Example

ShipmentService

---

Controllers

EntityNameController

---

DTOs

CreateShipmentRequest

ShipmentResponse

UpdateShipmentRequest

---

Exceptions

ValidationException

BusinessRuleException

NotFoundException

ConflictException

UnauthorizedException

---

Events

Past tense.

ShipmentCreated

ShipmentAssigned

ShipmentDelivered

---

Database

Tables

snake_case

Columns

snake_case

Primary Keys

UUID

Foreign Keys

fk_<table>_<table>

Indexes

idx_<table>_<column>

---

REST API

Plural nouns

/api/shipments

/api/users

---

HTTP Methods

GET

POST

PUT

DELETE

PATCH only when necessary.

---

Branch Strategy

main

develop

feature/<name>

bugfix/<name>

hotfix/<name>

---

Commit Convention

feat:

fix:

docs:

test:

refactor:

perf:

ci:

build: