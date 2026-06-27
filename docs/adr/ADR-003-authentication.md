# ADR: Estrategia de seguridad mediante JWT, Refresh Token y Revocación Activa
## Estatus
### Aceptado
## Contexto
En nuestra arquitectura de microservicios, el componente de C# actua como proveedor de identidad. Los microservicios de Scala y Java deben ser capaces de validad de forma independiente si una petición HTTP esta autenticada, sin necesidad de consultar al servicio de C# en cada llamada.

Sin embargo, debido a que los JWT son stateless y no se pueden destruir antes de su fecha de expiracion, el sistema de logistica require de un mecanismo para revocar acceso de forma inmediata, manteniendo una experiencia de usuario fluida mediante sesiones prolongadas.

## Decisión
Se decide implementar un esquema de autentificación hibrido basado en Access Tokens JWT Short-Lived, Refresh Tokens Stored Long-Lived y un Mecanismo de Revocación Activa Blacklist gestionado por el microservicio de C#

### Detalles Tecnicos del Flujo:
+ Access Token JWT: Emitido por C#. Tendra un tiempo de vida de 15 minutos, Almacena los roles y permisos del Usuario. Los servicios de Scala y Java lo validan de forma matematica de manera local usando la llave publica.
* Refresh Token: Un string aleatorio de alta entropia emitido junto al JWT, con un tiempo de vida largo de 7 dias. Este token es utilziado en una base de datos rapida Redis o PostgreSQL controlada por el servicio de C#. Permite solicitar un nuevo access token sin pedir nuevamente credenciales.
* Mecanismo de revocacion Cierre de Sesión o Bloqueo: Cuando se solicita revocar el acceso, el servicio C# elimina el Refresh Token de la BD e incluye el ID del JWT en una Blacklist en memoria compartida como Redis o PostgreSQL.
+ + El tiempo de permanencia de un token en la blacklist sera igual al tiempo de vida restante del Access Token 15 minutos
## Alternativas Consideradas
+ JWT puramente Stateless sin Refresh Tokens: Rechazado. Mitigando el riesgo de token robado, la vida de JWT debe ser corta obligando al usuario a introducir las credenciales constantemente y arruinando la experiencia del usuario.
+ Validacion Sincrona(Stateful Tokens): Rechazado. Obliga a Scala y Java a realizar una peticion http a C# para verificar la validez de cada token destruyendo el rendimiento y autonomia del microservicio.
## Consecuencias
### Positivas
+ Autonomia y Velocidad: Scala y Java validan los accesos en microsegundos usando criptografia, sin dependencia de red de otros servicios.
+ Seguridad Avanzada (Revocacion inmediata): En caso de emergencia, un token puede ser invalidado globalmente en menos de 15 minutos (vida del JWT) bloqueando su Refresh Token al instante.
+ Sesiones Prolongadas Seguras: Los usuarios no necesitan ingresar constantemente sus credenciales gracias al uso del Refresh Token
### Negativas
+ Complejidad de Implementación. EL cliente debe manejar la logica y manejar los errores de expiracion y solicitar la renovación del token en segundo plano.
+ Introducción de Estado (Stateful Edge): El servicio de C# ahora depende de una capa de persistencia Redis o PostgreSQL para trazar los Refresh Tokens y la Blacklist, rompiendo la naturaleza stateless de los JWT.
+ Validacion de Blacklist: Si el negocio requiere revocacion absoluta, los servicios de Scala y Java tendran que consultar rapidamente la Blacklist para procesar la petición, añadiendo lectura de red al flujo.