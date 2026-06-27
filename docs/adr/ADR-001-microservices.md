# ADR: Arquitectura de Microservicios Poliglota y Desacoplada
## Estatus
### Aceptado
## Contexto
El sistema de logistica requiere resolver tres necesidades de negocio con naturalezas técnias muy distintas:
+ Una capa de seguridad de alta disponibilidad y baja latencia.
+ Un nucleo transaccional complejo para todo el ciclo de vida de los envios con reglas estrictas de negocio y necesidades de auditoria inmutable.
+ Un motor de procesamiento analitico y calculo de metricas operacionales sobre los eventos de negocio
Desarrollar todo el sistema en un monolito o bajo un solo lenguaje de programación limitara la eficacia con la que cada componente resuelve su problema especifico.
## Decisión
Se decide implementar una Arquitectura de Microservicios Poligrota, distribuyendo las responsabilidades del sistema de logistica en componentes independientes utilziando el lenguaje idoneo para cada dominio:
+ Microservicio de Autentificación (C#): Elegido por su velocidad de ejecución, ecosistema nativo de seguridad y excelente rendimiento en operaciones Input/Output para la emision y validacion de identidades.
+ Microservicio de Gestion de Envios y Auditoria (Scala) : Elegido debido a que el paradigma funcional de Scala, su sistema de tipos avanzados y la inmutabilidad nativa minimizan los errores en las maquinas de estado complejas como el ciclo de vida del envio y facilitan la creacion de los registros de auditoria consistentes.
+ Microservicio de Metricas y Analitica (Java): Elegido por su ecosistema maduro en procesamiento de datos, facilidad de integración con herramientas de persistencia analitica y la robustez en Spring AMQP para procesamiento de datos en segundo plano.

La comunicacion entre estos servicios sera asincrona y basada en eventos, excepto las validaciones de identidad directas del API Gateway que seran sincronas.

## Alternativas Consideradas
+ Monolito Poliglta: Rechazado. Aunque permite multiples lenguajes en la misma JVM, mantiene un acoplamiento al despliegue e infraestructura, impidiendo el escalado independiente de cada modulo.
+ Microservicio Homogeneo: Rechazado. Usar un solo lenguaje sacrifica las ventajas de programacion funcional de Scala para auditoria y control de estados o la ligereza de autentificacion de .NET
## Consecuencias
### Positivas
+ Optimización por Dominio: Cada microservicio aprovecha las fortalezas máximas de su respectivo lenguaje y framework.
+ Escalabilidad independiente: Si el servicio de metricas requiere procesar reportes masivos se puede escalar horizontalmente en la infraestructura sina fectar los envios o el inicio de sesion.
+ Ciclos de Despliegue Autonomos: Los equipos pueden desplegar actualizaciones en el servicio de autentificación sin necesidad de compilar o detener los servicios de logistica.
### Negativas
+ Complejidad Operativa: El pipeline de CI/CD debe soportar 3 entornos de construccion distintos SBT Scala, Maven Java y .NET para C# ademas de requerir monitore distribuido.
+ Curva de Aprendizaje: El equipo de desarrollo debe tener conocimientos multi lenguaje o organizado en celulas de especializacion, lo que fragmenta el conocimiento del sistema completo