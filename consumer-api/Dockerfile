FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent pom and module poms
COPY pom.xml .
COPY producer-api/pom.xml producer-api/
COPY consumer-api/pom.xml consumer-api/

# Download dependencies
RUN mvn dependency:go-offline -pl consumer-api -am

# Copy source code
COPY consumer-api/src consumer-api/src

# Build the application
RUN mvn clean package -pl consumer-api -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the jar from build stage
COPY --from=build /app/consumer-api/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]