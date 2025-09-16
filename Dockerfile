# Multi-stage build: compile with Maven, run with JRE
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/mpxj-convert.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]

