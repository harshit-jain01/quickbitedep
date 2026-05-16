FROM ghcr.io/library/maven:3.9-eclipse-temurin-21 AS build

ARG SERVICE_DIR=backend/delivery-service

WORKDIR /workspace
COPY ${SERVICE_DIR}/pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY ${SERVICE_DIR}/src ./src
RUN mvn -B -ntp -Dmaven.test.skip=true package && \
    jar_file="$(find target -maxdepth 1 -name '*.jar' ! -name 'original-*' | head -n 1)" && \
    cp "$jar_file" app.jar

FROM ghcr.io/adoptium/temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/logs && chown -R spring:spring /app/logs
COPY --chown=spring:spring --from=build /workspace/app.jar /app/app.jar
USER spring

ENV JAVA_OPTS=""
EXPOSE 8087
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
