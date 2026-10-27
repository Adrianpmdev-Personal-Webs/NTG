# Stage 1: Build (Maven + JDK 25)
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace

# Opciones de la JVM de Maven (permite a Lombok compilar en Java 25 sin avisos)
COPY .mvn .mvn

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

# Stage 2: Runtime (Java 25 JRE, ya incluye los certificados para HTTPS)
FROM eclipse-temurin:25-jre

# Usuario sin privilegios: si alguien llegara a ejecutar código en el contenedor, no sería root
RUN groupadd --system app && useradd --system --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

# Copiar el JAR generado (de root y solo de lectura para el usuario de la aplicación)
COPY --from=build /workspace/app/target/ntg.jar ntg.jar

USER app

# Exponer puerto
EXPOSE 8080

# Comando de inicio. Ajustes para los 512 MB del plan gratuito de Render:
#  - MaxRAMPercentage=70: el heap usa como mucho el 70 % de la memoria; el resto queda para la JVM y el sistema
#  - UseSerialGC: el recolector más ligero, el adecuado con 1 CPU y poca memoria
#  - ExitOnOutOfMemoryError: si se queda sin memoria se cierra y Render lo arranca de nuevo, en vez de quedarse colgado
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-XX:+ExitOnOutOfMemoryError", "-jar", "ntg.jar"]
