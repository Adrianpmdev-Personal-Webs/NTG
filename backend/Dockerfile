# Stage 1: Build (Maven + JDK 25)
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copiar todos los pom.xml
COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY pokemon/pom.xml pokemon/pom.xml
COPY wow-forever/pom.xml wow-forever/pom.xml

# Copiar todo el código fuente
COPY core/src core/src
COPY pokemon/src pokemon/src
COPY wow-forever/src wow-forever/src

# Build: compila core (y el resto de modulos como dependencias)
RUN mvn -pl core -am clean package -DskipTests

# Stage 2: Runtime (Java 25 JRE)
FROM eclipse-temurin:25-jre
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /core

# Copiar el JAR generado
COPY --from=build /workspace/core/target/core-0.1.jar core.jar

# Exponer puerto
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["java", "-jar", "core.jar"]