# Etapa 1: Construcción (Build) con SBT
FROM hseeberger/scala-sbt:11.0.11_1.5.5_2.13.6 AS build
WORKDIR /app
COPY build.sbt ./
COPY project ./project

# Cacheamos las dependencias de Scala
RUN sbt update 
COPY . .

# CAMBIO AQUÍ: Usamos 'stage' que es el comando nativo de Play Framework
RUN sbt stage
# ... (La Etapa 1 de construcción se queda exactamente igual) ...

# Etapa 2: Imagen de ejecución (Runtime) - Ligera
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app

# CLAVE: Instalar bash en Alpine para que los scripts de Play Framework funcionen
RUN apk add --no-cache bash

# Copiamos los archivos compilados por Play Framework
COPY --from=build /app/target/universal/stage ./stage

# Aseguramos permisos de ejecución en toda la carpeta bin
RUN chmod -R 755 /app/stage/bin

# Puerto en el que escucha el servicio de Scala (Play Framework)
EXPOSE 9000

# Ejecutamos el script usando bash de manera explícita evitando el comodín directo
ENTRYPOINT ["/bin/bash", "-c", "/app/stage/bin/$(ls /app/stage/bin/ | grep -v '\\.') -Dplay.http.secret.key=un-secreto-temporal-para-pruebas -Dhttp.port=9000"]