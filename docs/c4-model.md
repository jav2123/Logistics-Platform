## Nivel 1: Contexto del Sistema (System Context)

Muestra a los actores humanos y cómo interactúan a alto nivel con el sistema de logística, abstrayendo la tecnología.

Actores:

+ Admin (Administrador): Consulta reportes avanzados, métricas de rendimiento y audita el estado global del sistema.

+ Dispatcher (Despachador): Crea, modifica y asigna los envíos/rutas desde la central.

+ Driver (Chofer): Actualiza el estado del envío (En Ruta, Entregado) desde la aplicación móvil.

+ Customer (Cliente): Consulta el estado de su paquete (Tracking).

Sistemas:

+ Logistics Platform (Sistema Central): El sistema de software que engloba toda la lógica de autenticación, gestión de envíos y analítica.

## Nivel 2: Contenedores (Containers)

Muestra las aplicaciones/servicios ejecutables y los almacenes de datos que componen el sistema, detallando tecnologías y protocolos de comunicación.

```
[Customer/Driver/Dispatcher/Admin]
               │
        (HTTP / HTTPS)
               ▼
        [API Gateway] 
               │
        ┌──────┴──────────────────────────┐
  (HTTP)│                                 │(HTTP)
        ▼                                 ▼
[Auth Service] (.NET)             [Shipment Service] (Scala)
  │                                       │
  ├─(SQL)─► [PostgreSQL (Auth DB)]        ├─(SQL)─► [PostgreSQL (Shipment DB)]
  │                                       │
  ▼                                       ▼
[Redis (Blacklist JWT)]            (AMQP)─► [RabbitMQ]
                                              │
                                        (AMQP)│
                                              ▼
                                   [Analytics Service] (Java)
                                              │
                                              └─(SQL)─► [PostgreSQL (Analytics DB)]
```
+ API Gateway: Punto de entrada único para los clientes. Encargado del ruteo inverso y de validar de forma perimetral el JWT en las peticiones entrantes.

+ Auth Service (.NET): Gestiona identidades, emite/refresca tokens y maneja la lista de revocación. Usa PostgreSQL para credenciales y el ciclo de vida del refresh_token, y Redis exclusivamente como Blacklist de JWTs inválidos de forma perimetral..

+ Shipment Service (Scala): Núcleo del negocio. Gestiona la creación de órdenes, asignación de rutas y auditoría del ciclo de vida del paquete. (Usa PostgreSQL para persistencia transaccional).

+ Analytics Service (Java): Consume los eventos del negocio de forma asíncrona para generar métricas de rendimiento. (Usa PostgreSQL o una BD de series temporales para el histórico de métricas).

+ RabbitMQ: Message Broker que actúa como el tejido de comunicación asíncrona (Pub/Sub) entre los servicios mediante un Topic Exchange.

## Nivel 3: Componentes (Components)

Desglose interno de los contenedores clave para entender su arquitectura de software (enfocado en controladores, servicios y repositorios).
Componentes de Auth Service (.NET)

+ AuthController: Expone los endpoints REST para Login, Logout y Refresh Token.

+ UserService: Valida las credenciales de los usuarios contra el repositorio.

+ TokenService: Genera los JWT de corta vida, los Refresh Tokens y gestiona la invalidación en Redis.

+ RoleService: Gestiona los permisos y roles de los actores.

+ UserRepository: Abstracción de acceso a la base de datos de usuarios (vía Entity Framework o Dapper).

Componentes de Shipment Service (Scala)

+ ShipmentController: Expone la API HTTP (Play Framework) para la creación y actualización de envíos.

+ ShipmentService: Contiene las reglas puras del negocio logístico (cambios de estado válidos, asignación de choferes).

+ DomainEventPublisher: Componente encargado de serializar a JSON y publicar los eventos (ej: ShipmentCreated) en RabbitMQ.

+ ShipmentRepository: Maneja la persistencia transaccional de los envíos.

Componentes de Analytics Service (Java)

+ EventConsumer (@RabbitListener): Escucha activamente las colas de RabbitMQ para capturar los eventos publicados por Scala.

+ MetricsAggregator: Procesa los eventos crudos para calcular KPIs (ej. Tiempo promedio de entrega).

+ ReportService: Expone la lógica para proveer los datos analíticos cuando el Administrador los solicita.

Flujo Principal y Trazabilidad (Secuencia)

Este flujo amarra perfectamente el diseño y demuestra cómo interactúan tus componentes en la práctica:

+ Autenticación: El usuario envía sus credenciales al API Gateway, que las redirige al AuthController (C#).

+ Emisión: El TokenService (C#) valida los datos usando UserRepository y genera un par de tokens (JWT + Refresh Token). El JWT regresa al cliente.

+ Petición Protegida: El Dispatcher crea un envío enviando un HTTP POST. La petición pasa por el API Gateway (que valida criptográficamente el JWT) y llega al ShipmentController (Scala).

+ Persistencia: El ShipmentService (Scala) procesa la orden y la guarda a través del ShipmentRepository.

+ Notificación: Inmediatamente después, el DomainEventPublisher (Scala) envía el mensaje envio.creado en formato JSON al Exchange de RabbitMQ.

+ Procesamiento Analítico: El EventConsumer (Java) recibe el mensaje desde su cola exclusiva en RabbitMQ, el MetricsAggregator actualiza las métricas y se guarda el estado analítico.

+ Consumo de Reportes: El Admin solicita un reporte de rendimiento. La petición viaja por el Gateway hasta el ReportService (Java), que entrega los resultados procesados.