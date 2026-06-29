# Authentication Strategy

## Goal

Provide secure authentication using JWT and Refresh Tokens.

---

# Login Flow

User

↓

Auth Service

↓

Validate Credentials

↓

Generate Access Token

↓

Generate Refresh Token

↓

Return Tokens

---

# JWT

Contains

- UserId
- Email
- Roles
- Expiration

Lifetime

15 minutes

---

# Refresh Token

Lifetime

7 days

Stored

Database

Characteristics

- Revocable
- Rotable
- Unique

---

# Authorization

Role Based Access Control (RBAC)

Roles

ADMIN

DISPATCHER

DRIVER

CUSTOMER

---

# Token Validation

Performed by

API Gateway

Shipment Service

Analytics Service (future)

---

# Password Storage

Algorithm

BCrypt

Never store passwords.

Only hashes.

---

# Logout

Invalidate Refresh Token.

JWT expires naturally.

---

# Security Headers

Enable

CORS

Disable

CSRF for stateless APIs.

Use HTTPS.

---

# Future Improvements

Multi-factor Authentication

OAuth2

OpenID Connect