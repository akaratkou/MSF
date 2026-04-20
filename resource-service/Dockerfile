FROM maven:3.9.12-eclipse-temurin-25-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true


FROM eclipse-temurin:25-jre-alpine-3.23 AS runner
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

# 
EXPOSE 8080


CMD ["java", "-jar", "app.jar"]
