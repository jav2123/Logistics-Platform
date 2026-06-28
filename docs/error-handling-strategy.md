# Error Handling Strategy

## Purpose

Provide consistent API responses across all services.

---

# Error Response

```json
{
    "timestamp":"...",
    "status":404,
    "error":"NOT_FOUND",
    "code":"SHIPMENT_NOT_FOUND",
    "message":"Shipment not found.",
    "path":"/api/shipments/10"
}
```

---

# Exception Mapping

| Exception | HTTP |
|------------|------|
| ValidationException | 400 |
| UnauthorizedException | 401 |
| ForbiddenException | 403 |
| NotFoundException | 404 |
| ConflictException | 409 |
| BusinessRuleException | 422 |
| InfrastructureException | 500 |
| UnexpectedException | 500 |

---

# Error Codes

AUTH_INVALID_CREDENTIALS

AUTH_TOKEN_EXPIRED

USER_NOT_FOUND

SHIPMENT_NOT_FOUND

SHIPMENT_ALREADY_ASSIGNED

INVALID_DRIVER

DATABASE_ERROR

UNKNOWN_ERROR

---

# Logging

400

Information

401

Warning

404

Warning

409

Warning

500

Error

---

# Validation Errors

Example

```json
{
  "status":400,
  "code":"VALIDATION_ERROR",
  "errors":[
      {
          "field":"origin",
          "message":"Origin is required."
      }
  ]
}
```

---

# Principles

Never expose

- Stack traces
- SQL errors
- Internal exception names

Always log

- Correlation ID
- User ID
- Timestamp
- Request Path

---

# Correlation ID

Every request receives

X-Correlation-ID

If absent

Generate one.

Return it in every response.