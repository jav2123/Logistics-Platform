# Domain Event Contracts

## Purpose
Describe every asynchronous event published by the platform. All events transit through RabbitMQ via a Topic Exchange.

---

## Common Metadata
Every event envelope MUST contain the following root metadata properties for tracing and routing:

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "ShipmentCreated",
  "occurredAt": "2026-06-27T17:45:00Z",
  "version": 1,
  "payload": {}
}

---

# ShipmentCreated

+ Producer: Shipment Service

+ Consumers: Analytics Service

Payload

```json
{
  "eventId": "8f5a12cd-7111-4b10-86b6-b530d959567e",
  "eventType": "ShipmentCreated",
  "occurredAt": "2026-06-27T17:45:00Z",
  "version": 1,
  "payload": {
    "shipmentId": "b7e283b1-0988-4f1b-9d41-471017ef36bb",
    "customerId": "1a2b3c4d-5e6f-7g8h-9i0j-1k2l3m4n5o6p",
    "status": "CREADO",
    "origin": {
      "street": "Av. Reforma 123",
      "city": "CDMX",
      "country": "México"
    },
    "destination": {
      "street": "Av. de la Constitución 456",
      "city": "Monterrey",
      "country": "México"
    }
  }
}
```

---

# ShipmentAssigned

+ Producer: Shipment Service

+ Consumers: Analytics Service

Payload

```json
{
  "eventId": "9a1b2c3d-4e5f-6g7h-8i9j-0k1l2m3n4o5p",
  "eventType": "ShipmentAssigned",
  "occurredAt": "2026-06-27T18:00:00Z",
  "version": 1,
  "payload": {
    "shipmentId": "b7e283b1-0988-4f1b-9d41-471017ef36bb",
    "driverId": "8f5a12cd-7111-4b10-86b6-b530d959567e",
    "status": "ASIGNADO"
  }
}
```

---

# ShipmentDelivered

+ Producer: Shipment Service

+ Consumers: Analytics Service
Payload

```json
{
  "eventId": "0a1b2c3d-4e5f-6g7h-8i9j-0k1l2m3n4o5p",
  "eventType": "ShipmentDelivered",
  "occurredAt": "2026-06-27T19:30:00Z",
  "version": 1,
  "payload": {
    "shipmentId": "b7e283b1-0988-4f1b-9d41-471017ef36bb",
    "driverId": "8f5a12cd-7111-4b10-86b6-b530d959567e",
    "status": "ENTREGADO",
    "deliveredAt": "2026-06-27T19:28:45Z"
  }
}
```

---

# ShipmentCancelled

+ Producer: Shipment Service

+ Consumers: Analytics Service
Payload

```json
{
  "eventId": "ef1b2c3d-4e5f-6g7h-8i9j-0k1l2m3n4o5p",
  "eventType": "ShipmentCancelled",
  "occurredAt": "2026-06-27T18:15:00Z",
  "version": 1,
  "payload": {
    "shipmentId": "b7e283b1-0988-4f1b-9d41-471017ef36bb",
    "status": "CANCELADO",
    "reason": "Cliente rechazó el paquete por daño externo.",
    "cancelledByUserId": "1a2b3c4d-5e6f-7g8h-9i0j-1k2l3m4n5o6p"
  }
}
```

---

# Event Versioning

Rules

- Never remove fields.
- New fields must be optional.
- Version changes only for breaking changes.

---

# Idempotency

Consumers must process duplicated events safely.

The eventId must be unique.