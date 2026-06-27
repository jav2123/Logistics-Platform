# Modelo de Dominio (Domain Model) - Sistema de Logística

## 1. Contexto del Dominio y Alcance

El sistema se enfoca exclusivamente en la gestión del ciclo de vida del envío (core transaccional) y en el análisis de rendimiento operacional del negocio.

### Responsabilidades por Microservicio:
* **Identidad y Acceso (C# / .NET):** Autenticación de usuarios, emisión/rotación de credenciales y asignación de roles específicos del sistema.
* **Core Logístico (Scala / Play Framework):** Gestión del estado físico y lógico del paquete, transiciones de la máquina de estados y auditoría inmutable del ciclo de vida del envío.
* **Métricas del Negocio (Java / Spring Boot):** Inteligencia de negocio asíncrona. Transforma los eventos de logística en métricas agregadas de rendimiento para la toma de decisiones.

### Exclusiones del Dominio (Fuera de Alcance):
* **Pasarela de Pagos:** No se procesan transferencias de dinero ni cargos a tarjetas. Se asume que el envío llega al sistema una vez el pago ha sido aprobado de forma externa.
* **Gestión de Flotas Avanzada:** No se gestiona información mecánica o de mantenimiento del vehículo (seguros, kilometraje, consumo de combustible, etc.).
* **E-commerce / Inventario:** El sistema procesa "envíos" de forma agnóstica sin importar el contenido físico, precios de productos o control de existencias en almacén.

---

## 2. Bounded Contexts (Contextos Delimitados)


```
   [ Auth Context ] 
          │
  (Emite JWT + Role)
          ▼
 [ Shipment Context ] ──(Eventos Asíncronos AMQP)──► [ Analytics Context ]

```


### Auth Context
Responsable de la gestión de identidades, control de acceso basado en roles (RBAC) y el ciclo de vida de las sesiones activas (tokens).

### Shipment Context
Responsable de la integridad de los envíos, validación de transiciones de estados del paquete, asignación de conductores y persistencia del historial de auditoría.

### Analytics Context
Responsable de la ingesta de eventos de negocio, cálculo de KPIs operacionales y agregación numérica de datos optimizada para la lectura.

---

## 3. Estructura de Agregados y Entidades

### Contexto: Auth (C#)

El usuario tiene un único rol asignado de forma obligatoria para simplificar las validaciones en el API Gateway. Las acciones de suspensión de usuario o cambio de rol impactan atómicamente a las sesiones activas del usuario.

* **Aggregate Root:** `User`
    * `Id` (UUID)
    * `Email` (String)
    * `PasswordHash` (String)
    * `Role` (Enum: `"ADMIN"`, `"DISPATCHER"`, `"DRIVER"`, `"CUSTOMER"`)
* **Entidad Interna:** `RefreshToken`
    * `TokenId` (String / Hashed)
    * `ExpiresAt` (DateTime)
    * `IsRevoked` (Boolean)

### Contexto: Shipment (Scala)

El envío y su historial de estados son indisolubles; no puede existir una auditoría huérfana de envío. El agregado no contiene la entidad completa del conductor, sino una referencia por identificador (`DriverId`).

* **Aggregate Root:** `Shipment`
    * `Id` (UUID)
    * `CurrentStatus` (Enum)
    * **Value Object:** `Address` (Origen y Destino - Inmutables)
    * `DriverId` (UUID / Opcional - Referencia débil a otro Agregado)
    * **Entidad Interna (Colección):** `ShipmentAudit` (Historial inmutable de cambios)
* **Aggregate Root:** `Driver`
    * `Id` (UUID)
    * `Status` (Enum: `"ACTIVO"`, `"DESCANSANDO"`, `"VACACIONES"`)

### Contexto: Analytics (Java)

Tablas optimizadas exclusivamente para lecturas masivas y promedios temporales.

* **Aggregate Root:** `DailyMetric`
    * `Date` (LocalDate - Clave primaria/índice)
    * `TotalVolume` (Integer)
    * `CancellationRate` (Float)
* **Aggregate Root:** `ShipmentMetric`
    * `ShipmentId` (UUID)
    * `WaitTimeInWarehouse` (Duration)
    * `TotalTransitTime` (Duration)

---

## 4. Reglas de Negocio (Invariantes)

### Auth Context
1.  **Regla de Rotación de Tokens:** Cada vez que un usuario utiliza un `RefreshToken` válido para obtener un nuevo `AccessToken`, el `RefreshToken` utilizado debe marcarse como invalidado (`isRevoked = true`) inmediatamente en la misma transacción y emitirse un nuevo par.
2.  **Invalidación Atómica por Administración:** Si un Administrador bloquea a un usuario o modifica su `Role`, todos los `RefreshTokens` activos asociados a ese `UserId` se revocan de forma inmediata dentro de la misma transacción de la base de datos.
3.  **Principio de Mínimo Privilegio:** Los contextos de *Shipment* y *Analytics* no validan credenciales ni contraseñas; asumen la validez criptográfica de la firma del JWT e inspeccionan el *claim* de `Role` para autorizar las acciones (ej. solo un usuario con rol `"DRIVER"` puede transicionar un envío a `"ENTREGADO"`).

### Shipment Context
1.  **Máquina de Estados Inmutable (Transición de Envíos):** Un envío debe seguir una secuencia lógica estricta de estados, impidiendo saltos abruptos en el flujo:
    $$\text{CREADO} \longrightarrow \text{ASIGNADO} \longrightarrow \text{EN TRÁNSITO} \longrightarrow \text{ENTREGADO}$$
2.  **Regla de Salida por Cancelación:** Un envío solo puede mutar al estado `CANCELADO` si su estado actual es `CREADO` o `ASIGNADO`. Si el paquete ya se encuentra `EN TRÁNSITO`, la regla de negocio prohíbe la cancelación directa por el flujo estándar.
3.  **Restricción de Modificación de Dirección:** La dirección de destino (`Address`) solo es mutable mientras el envío se encuentre en estado `CREADO` o `ASIGNADO`. Una vez pasa a `EN TRÁNSITO`, el sistema arroja un error de negocio.
4.  **Consistencia del Historial Obligatorio:** Toda mutación en el Agregado `Shipment` (cambio de estado, reasignación de chofer o cambio de dirección) genera obligatoriamente una nueva entrada en la lista interna de `ShipmentAudit`. No se permiten cambios silenciosos.
5.  **Desacoplamiento de Entidades (Referencia por Identidad):** El agregado `Shipment` solo almacena el `DriverId`. Antes de realizar una asignación, el sistema debe validar mediante eventos previos o consultas al agregado `Driver` que el conductor se encuentre en estado `"ACTIVO"`.

### Analytics Context
1.  **Idempotencia Tolerante a Duplicados:** Debido a que el Message Broker garantiza la entrega de eventos *"al menos una vez"* (at-least-once), el servicio de Java debe validar si el ID único del evento ya fue procesado antes de alterar o sumar datos a las agregaciones numéricas de los gráficos.
2.  **Aislamiento Analítico:** El almacenamiento de analítica está aislado de la operación. Ningún bloqueo, lentitud o fallo en la base de datos de métricas de Java debe interferir o degradar la capacidad de Scala para registrar o modificar envíos.
3.  **Regla de Ventana de Tiempo (Event Time):** Las agregaciones dentro del Agregado `DailyMetric` se calculan utilizando el `Timestamp` original en el que ocurrió el evento en el servicio de Scala, y no la hora de llegada del mensaje al servicio de Java. Esto evita distorsiones si el broker experimenta retrasos en la entrega.

---

## 5. Comunicación Asíncrona y Eventos de Dominio

Se prohíben las consultas directas entre las bases de datos de los distintos microservicios. La sincronización e ingesta de datos ocurre a través de los siguientes eventos de dominio publicados en RabbitMQ:

* **`ShipmentCreated`**: Publicado por Scala al registrar un nuevo paquete. Contiene `shipmentId`, `customerId`, `address` y `timestamp`.
* **`ShipmentAssigned`**: Publicado por Scala al vincular un chofer al paquete. Contiene `shipmentId`, `driverId` y `timestamp`.
* **`ShipmentDelivered`**: Publicado por Scala cuando el chofer confirma la entrega. Contiene `shipmentId`, `driverId` y `timestamp`.
* **`ShipmentCancelled`**: Publicado por Scala ante una cancelación válida. Contiene `shipmentId`, `motivo` y `timestamp`.