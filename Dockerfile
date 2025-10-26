# Multi-stage Dockerfile for building and running LocalNews
# Builder stage: uses Maven with JDK 21 to build the fat jar
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy only the files needed to build to leverage Docker layer caching
COPY pom.xml ./
COPY src ./src

# Build the project (skip tests to speed up builds; adjust if you want tests during build)
RUN mvn -B -DskipTests package

### Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the builder stage (pom.xml sets finalName to local-news)
COPY --from=builder /workspace/target/local-news.jar /app/local-news.jar

# Optional: allow passing JVM options at runtime
ENV JAVA_OPTS=""

# Expose default port (Render provides $PORT at runtime). Spring Boot will use --server.port.
EXPOSE 8080

# Use shell form to allow environment variable expansion for PORT at container start
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/local-news.jar --server.port=${PORT:-8080}"]
