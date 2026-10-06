# Build stage: ubi9/openjdk-17 ships with Maven
FROM registry.access.redhat.com/ubi9/openjdk-17:latest AS build
USER root
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

# Runtime stage
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:latest
COPY --from=build /build/target/offers-service.jar /deployments/app.jar
EXPOSE 8080
USER 185
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=70 $JAVA_OPTS -jar /deployments/app.jar"]
