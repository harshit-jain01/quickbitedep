FROM maven:3.9-eclipse-temurin-21 AS build

ARG SERVICE_DIR=OnlineFoodDeliveryPlatform-Backend-main/menu-service

WORKDIR /workspace
COPY ${SERVICE_DIR}/pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY ${SERVICE_DIR}/src ./src
RUN mvn -B -ntp -DskipTests package && \
    jar_file="$(find target -maxdepth 1 -name '*.jar' ! -name 'original-*' | head -n 1)" && \
    cp "$jar_file" app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/app.jar /app/app.jar
USER spring

ENV JAVA_OPTS=""
EXPOSE 9093
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
