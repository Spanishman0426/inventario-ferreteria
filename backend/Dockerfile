# Nombre: Dockerfile
# Descripción: Imagen del backend en dos etapas: compila el JAR con el Maven Wrapper (sin pruebas, que ya
#              corren en ./mvnw verify) y lo ejecuta con un JRE 21 como usuario sin privilegios.
#              La usan docker-compose.e2e.yml y el Web Service de Render (perfil prod, 512 MB de RAM).
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
# Memoria para el plan gratuito de Render (512 MB): el heap usa hasta el 75 % del límite del contenedor;
# SerialGC y pilas de 512 KB reducen la memoria fuera del heap; ante un OutOfMemoryError la JVM termina
# y la plataforma reinicia el servicio en lugar de dejarlo colgado. La JVM lee JAVA_TOOL_OPTIONS sola.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=compilacion /fuente/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=America/Bogota", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
