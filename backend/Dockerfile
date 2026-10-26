# ---- build stage: compile and package the jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- run stage: small image with only the JRE and the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home appuser && mkdir /app/uploads && chown appuser /app/uploads
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
