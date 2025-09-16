# Multi-stage build: compile with Maven, run with JRE
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY settings.xml ./
COPY pom.xml ./
# Pre-fetch dependencies to improve stability on weak networks
RUN --mount=type=cache,target=/root/.m2 \
    mvn -s /src/settings.xml -q -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=5 -Daether.connector.http.retryHandler.count=5 \
    dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -s /src/settings.xml -q -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=5 -Daether.connector.http.retryHandler.count=5 \
    package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/mpxj-convert.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]

