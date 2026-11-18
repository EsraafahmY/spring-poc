# One multi-stage Dockerfile for every module:
#   docker build --build-arg MODULE=product-service -t spring-poc/product-service:1.0.0 .
# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY pom.xml .
COPY config-server/pom.xml config-server/
COPY discovery-server/pom.xml discovery-server/
COPY api-gateway/pom.xml api-gateway/
COPY product-service/pom.xml product-service/
COPY order-service/pom.xml order-service/
# cache dependencies in their own layer (only re-downloaded when a pom changes)
RUN mvn -B -q -pl ${MODULE} -am dependency:go-offline
COPY . .
RUN mvn -B -q -pl ${MODULE} -am package -DskipTests \
 && java -Djarmode=tools -jar ${MODULE}/target/app.jar extract --layers --launcher --destination /layers

# ---------- Stage 2: small runtime image (JRE only, non-root) ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /app
# Spring Boot layered jar: dependencies change rarely, so they get their own cached layers
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
