# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build

ARG MODULE
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY user/pom.xml user/pom.xml
COPY oauth/pom.xml oauth/pom.xml

RUN mvn -B -ntp -pl "${MODULE}" -am dependency:go-offline

COPY common/src common/src
COPY ${MODULE}/src ${MODULE}/src

RUN mvn -B -ntp -pl "${MODULE}" -am package

FROM eclipse-temurin:17-jre

ARG MODULE
ENV TZ=Asia/Shanghai
WORKDIR /app

COPY --from=build "/workspace/${MODULE}/target/${MODULE}-1.0-SNAPSHOT.jar" /app/app.jar

EXPOSE 9000 9001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
