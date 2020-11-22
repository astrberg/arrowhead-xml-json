FROM openjdk:11-jre-slim

WORKDIR /opt

COPY target/*-jar-with-dependencies.jar ./echo.jar