
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build


COPY pom.xml .
RUN mvn dependency:go-offline -B -q


COPY src ./src
RUN mvn clean package -DskipTests -B -q


FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app


RUN touch /app/.env


RUN addgroup -S trapegroup && adduser -S trapeuser -G trapegroup && chown -R trapeuser:trapegroup /app
USER trapeuser


COPY --from=build --chown=trapeuser:trapegroup /build/target/*.jar app.jar

EXPOSE 8080

# Container-aware JVM memory options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]