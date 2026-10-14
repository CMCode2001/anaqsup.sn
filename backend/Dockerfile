# syntax=docker/dockerfile:1.7
# Contexte de build : RACINE du dépôt (le contrat OpenAPI de contracts/ est embarqué).
#   docker build -f backend/Dockerfile -t anaqsup-backend .

FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f backend/pom.xml dependency:go-offline
COPY contracts/openapi contracts/openapi
COPY backend/src backend/src
# Les tests sont exécutés par la CI avant la construction de l'image.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f backend/pom.xml package -DskipTests \
 && java -Djarmode=tools -jar backend/target/anaqsup-backend.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S -G app -u 10001 app \
 && mkdir -p /var/lib/anaqsup/documents && chown app:app /var/lib/anaqsup/documents
WORKDIR /app
# Couches de la moins à la plus volatile : seules les classes changent d'une version à l'autre.
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./

ARG VERSION=dev
LABEL org.opencontainers.image.title="anaqsup-backend" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.source="https://github.com/CMCode2001/anaqsup.sn"
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"

USER 10001
EXPOSE 8080 8081
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
