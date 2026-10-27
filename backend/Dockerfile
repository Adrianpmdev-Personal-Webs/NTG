# Stage 1: Build (Maven + JDK 25)
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copiar todos los pom.xml
COPY pom.xml .
COPY platform-api/pom.xml platform-api/pom.xml
COPY core/pom.xml core/pom.xml
COPY games/pokemon/pom.xml games/pokemon/pom.xml
COPY games/wow-forever/pom.xml games/wow-forever/pom.xml
COPY app/pom.xml app/pom.xml

# Copiar todo el código fuente
COPY platform-api/src platform-api/src
COPY core/src core/src
COPY games/pokemon/src games/pokemon/src
COPY games/wow-forever/src games/wow-forever/src
COPY app/src app/src

# Build: compila app (y core, los juegos y platform-api como dependencias)
RUN mvn -pl app -am clean package -DskipTests

# Stage 2: Runtime (Java 25 JRE)
FROM eclipse-temurin:25-jre
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiar el JAR generado
COPY --from=build /workspace/app/target/ntg.jar ntg.jar

# Exponer puerto
EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["java", "-jar", "ntg.jar"]
