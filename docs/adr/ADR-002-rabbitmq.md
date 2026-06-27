# ADR: Selección de RabbitMQ para la comunicación asíncrona políglota
## Estatus
### Aceptado
## Contexto
Estamos diseñando un sistema de logística que implementa una aruitectura de microservicios poligrota:
+ C# (ASP.NET Core): Encargado de la Autentificación y seguridad perimetral.
+ Scala (Play Framework): Encargado del núcleo transaccional, gestión de enviós y auditoría.
+ Java (Spring Boot): Encargado de la ingesta de métricas y análisis de eventos de negocio.

El sistema requiere un mecanismo de comunicación asíncrona para notificar los cambios en el ciclo de vida de los envios desde Scala hacia Java para analítica, sin generar un acoplamiento directo entre los servicios a su vez basado en el principio de Database per Service.

El foco del sistema actual está orientado a flujos de trabajao basados en tareas y estado del paquete, y no al procesamiento de telemetría o coordenadas GPS en tiempo real.

## Decisión
Se decide utilizar RabbitMQ como el Message Broker oficial del proyecto, implementado en patrón de mensajería Pub/Sub (Publicación/Subscripción) mediante un TopicExchange.

Justificación Técnica:
+ Enrutamiento Flexible(Topics): Permite que el servicio de Scala publique eventos con llaves estructuradas y que Java escuche selectivamente mediante comodines
+ Aislamiento de ConsumidoreS: Cada microservicio tendrá su propia cola dedicada conectada al Exchange. Si el servicio de métricas de Java se cae por mantenimiento, los mensajes se acumularan en su cola de forma aislada sin afectar la operacion transaccional de Scala.
+ Ecosistema y Madurez: RabbitMQ posee drivers nativos, maduros y de alto rendimiento para el ecosistema poligrota del proyecto

## Alternativas consideradas
+ Kafka: Rechazado debido a que el sistema actual no requiere de streaming de datos masivos como telemetria GPS continua. La complejidad de configurar y mantener un Kluster supera las necesidades actuales de mensajeria basada en estados.
+ REST síncrono : Rechazado debido a un fuerte acoplamiento temporal. Si el servicio de Java sufre una ciada, bloque el flujo de creacion de Scala.
+ gRPC: Rechazado para la comunicacion general. Aunque sea ultra veloz, sigue siendo sincrono, no resuelve el problema de disponibilidad y persistencia temporal de mensajes que el negocio requiere.

## Consecuencias
### Positivas
+ Desacoplamiento Total: Los tres lenguajes coexisten sin conocer la ubicacion, direccion IP´o estado de disponibilidad de los demas.
+ Escalabilidad Elástica: Permite escalar de forma independiente los consumidores de Java si el procesamiento de métricas se vuelve pesado, o los de Scala si existe un pico en registros de auditoria sobre los envios.
+ Resilencia (Garantia de Entrega): Se implementara el reconocimiento manual de mensajes. Los mensajes no se borraran de RabbitMQ hasta que el consumidor confirme que se procesaron con exito.
### Negativas
+ Complejidad de Infraestructura: Añade una piueza critica de la infraestructura que debe ser monitoreada, clusterizada y mantenida en produccion.
+ Consistencia eventual: Las metricas en Java no se actualizaran al mismo milisegundo enq ue ocurre un cambio en scala; habra un retraso minimo de red y procesamiento. El sistema debe tolerar el desfase temporal.
+ Riesgo de Mensajes Duplicados: Ante fallos de red, RabbitMQ garantiza la entrega de mensajes al menos una vez, lo que obliga que el servicio de Java implemente logica idempotente en el consumo de mensajes.
## Notas de Implementación
+ Formato Universal: Toda comunicacion se realizara estrictamente en formato JSON con codificación UTF-8
+ Manejo de Errores: Se configura un Dead Letter Exchange para capturar los mensajes corruptos o fallidos, evitando que bloqueen las colas principales del sistema de logistica.