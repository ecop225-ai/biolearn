# =====================================================================
# BioLearn · Imagen del backend (Spring Boot 3.3.4, Java 17)
# La usa despliegue/docker-compose.yml; también sirve sola:  docker build -t biolearn-backend .
# La configuración (base de datos, JWT, CORS, correo) llega por variables de entorno desde
# despliegue/.env: application.properties no se sube a GitHub y NO va dentro de la imagen.
# =====================================================================

# ---- Etapa 1: compilación con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
# Las pruebas (JUnit, Selenium) se ejecutan en el equipo de desarrollo, no al construir la imagen del servidor.
RUN mvn clean package -Dmaven.test.skip=true -B

# ---- Etapa 2: imagen final, solo con el JRE ----
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=America/Bogota
COPY --from=build /app/target/biolearn-backend-0.0.1-SNAPSHOT.jar app.jar
# Comprobantes de pago y PDF de los cursos (biolearn.archivos.ruta=uploads): en el servidor es un volumen de Docker.
RUN mkdir -p /app/uploads

ENV JAVA_OPTS="-Xms256m -Xmx1024m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=America/Bogota -Dserver.port=${PORT:-8080} -jar app.jar"]
