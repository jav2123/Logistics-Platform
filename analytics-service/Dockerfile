# Etapa 1: Construcción - Actualizado a Gradle 9.0 para soportar Spring Boot 4.x
FROM gradle:9.0-jdk21 AS build
WORKDIR /app

# Copiamos archivos de configuración de Gradle
COPY build.gradle settings.gradle ./
# COPY gradlew ./
# COPY gradle ./gradle

# Descargamos dependencias para aprovechar la caché de capas de Docker
RUN gradle dependencies --no-daemon || true

# Copiamos el código fuente
COPY src ./src

# Compilamos el .jar omitiendo los tests
RUN gradle bootJar --no-daemon -x test

# Etapa 2: Imagen de ejecución (Runtime) - Temurin Java 21 (Ligera)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el .jar generado en la etapa anterior
COPY --from=build /app/build/libs/*.jar ./analytics-service.jar

# Puerto de escucha del microservicio
EXPOSE 8081

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "analytics-service.jar"]