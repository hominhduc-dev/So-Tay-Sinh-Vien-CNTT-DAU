# Giai đoạn 1: build bằng Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Giai đoạn 2: chạy bằng JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir -p /data/storage && chown app /data/storage
COPY --from=build /src/target/tangthu-server-*.jar app.jar
USER app
ENV STORAGE_DIR=/data/storage
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
