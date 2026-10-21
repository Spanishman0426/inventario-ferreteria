# Nombre: Dockerfile
# Descripción: Imagen del backend en dos etapas: compila el JAR con el Maven Wrapper (sin pruebas, que ya
#              corren en ./mvnw verify) y lo ejecuta con un JRE 21 como usuario sin privilegios.
#              La usa docker-compose.e2e.yml.
# Fecha de creación: 2026-10-21
# Última actualización: 2026-10-21
# Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)

# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /fuente

# Primero el pom y el wrapper: la descarga de dependencias queda en caché mientras no cambie el pom
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Desde un checkout de Windows el script puede perder el permiso de ejecución
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests -Djacoco.skip=true \
    && cp target/inventario-*.jar /fuente/app.jar

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre
ENV TZ=America/Bogota
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=compilacion /fuente/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=America/Bogota", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
