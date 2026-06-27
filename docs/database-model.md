# Modelo de Base de Datos (Database Model) - Plataforma de Logística

## 1. Principios de Diseño
* **Database per Service:** Cada microservicio posee y controla su propia base de datos independiente.
* **No Foreign Keys Cross-Service:** Está estrictamente prohibido crear llaves foráneas (`REFERENCES`) que apunten a tablas de otros microservicios. La consistencia se maneja de forma lógica y asíncrona.
* **Indexes Explícitos:** Todas las columnas utilizadas frecuentemente en filtros (`WHERE`), ordenamiento (`ORDER BY`) o joins lógicos poseen índices dedicados.
* **Auditing Tables:** Los cambios en las máquinas de estado críticas se guardan de forma inmutable.

---

## 2. Auth DB (Microservicio C# / .NET)

Diseño simplificado con rol directo en la entidad de usuario (RBAC) y soporte nativo para la rotación y revocación atómica de sesiones activas.

```sql
-- Tabla de Usuarios
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- "ADMIN", "DISPATCHER", "DRIVER", "CUSTOMER"
    is_blocked BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Refresh Tokens (Entidad dependiente del agregado User)
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE, -- Almacenado criptográficamente por seguridad
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Índices Explícitos
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id) WHERE is_revoked = FALSE;

```

---

## 3. Shipment DB (Microservicio Scala / Play Framework)

Estructura transaccional del negocio. Implementa la dirección (`Address`) como un Value Object aplanado en columnas y una tabla de auditoría cronológica ultra rápida.

```sql
-- Tabla de Envíos
CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL, -- Referencia lógica al cliente externo (sin FK)
    assigned_driver_id UUID,   -- Referencia lógica al conductor externo (sin FK)
    status VARCHAR(50) NOT NULL, -- "CREADO", "ASIGNADO", "EN_TRANSITO", "ENTREGADO", "CANCELADO"
    
    -- Origen (Value Object)
    origin_street VARCHAR(255) NOT NULL,
    origin_city VARCHAR(100) NOT NULL,
    origin_country VARCHAR(100) NOT NULL,
    
    -- Destino (Value Object)
    destination_street VARCHAR(255) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    destination_country VARCHAR(100) NOT NULL,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Auditoría Inmutable (Historial de Estados)
CREATE TABLE shipment_audit (
    id BIGSERIAL PRIMARY KEY, -- BIGSERIAL para alta velocidad de inserción
    shipment_id UUID NOT NULL,
    status_from VARCHAR(50),  -- NULL en el evento de creación inicial
    status_to VARCHAR(50) NOT NULL,
    changed_by_user_id UUID NOT NULL, -- ID del actor que gatilló el cambio
    cancellation_reason TEXT, -- Obligatorio solo si status_to = 'CANCELADO'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE
);

-- Índices Explícitos
CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_driver ON shipments(assigned_driver_id) WHERE assigned_driver_id IS NOT NULL;
CREATE INDEX idx_shipment_audit_shipment ON shipment_audit(shipment_id);

```

---

## 4. Analytics DB (Microservicio Java / Spring Boot)

Modelo de base de datos optimizado para consultas analíticas de lectura masiva. Incorpora mecanismos para garantizar la idempotencia de la red y el cálculo exacto por *Event Time*.

```sql
-- Tabla de Métricas por Envío (Tiempos de Ciclo de Vida)
CREATE TABLE shipment_metrics (
    shipment_id UUID PRIMARY KEY, -- Clave primaria (Relación lógica 1:1 con Shipment Core)
    last_processed_event_id UUID NOT NULL, -- Control estricto de Idempotencia / Deduplicación
    warehouse_wait_duration INTERVAL, -- Tiempo transcurrido entre CREADO y EN_TRANSITO
    total_transit_duration INTERVAL,   -- Tiempo transcurrido entre EN_TRANSITO y ENTREGADO/CANCELADO
    final_status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Métricas Diarias Agregadas (Atomicidad de Gráficos)
CREATE TABLE daily_metrics (
    metric_date DATE PRIMARY KEY, -- Basado estrictamente en la fecha original del evento en Scala
    total_shipments_created INT DEFAULT 0 NOT NULL,
    total_shipments_delivered INT DEFAULT 0 NOT NULL,
    total_shipments_cancelled INT DEFAULT 0 NOT NULL,
    avg_delivery_time_seconds DOUBLE PRECISION DEFAULT 0.0 NOT NULL
);

-- Índices Explícitos
CREATE INDEX idx_shipment_metrics_status ON shipment_metrics(final_status);
-- NOTA: 'daily_metrics' no requiere índice manual en 'metric_date' ya que PostgreSQL 
-- crea un índice B-Tree de forma automática para todas las llaves primarias (PRIMARY KEY).

```

---

## 5. Trazabilidad de Reglas de Negocio en la Base de Datos

1. **Idempotencia de Mensajes (Java):** Cuando el microservicio de Java consume un evento desde RabbitMQ, realiza una validación atómica: si el ID del evento coincide con `last_processed_event_id` en `shipment_metrics`, el mensaje se descarta y no altera los contadores de `daily_metrics`. Esto evita gráficos duplicados causados por la red.
2. **Rotación y Revocación (C#):** La consulta de revocación masiva por administrador se resuelve con un único comando indexado: `UPDATE refresh_tokens SET is_revoked = true WHERE user_id = :id`.
3. **Métricas por Event Time (Java):** El campo `metric_date` de la tabla `daily_metrics` se rellena usando el campo `timestamp` enviado en el *payload* del mensaje de Scala. Si un mensaje se retrasa horas en RabbitMQ debido a una caída de infraestructura, al procesarse se indexará en el día correcto del pasado y no en el día actual del servidor de Java.
