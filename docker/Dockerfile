ARG TAG="25.0.3_9-jre-alpine-3.23"

FROM docker.io/eclipse-temurin:${TAG}

LABEL org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.source="https://github.com/tmskln/spring-openproject-mcp-server.git" \
      org.opencontainers.image.description="MCP Server for OpenProject API (workpackages)"

WORKDIR /app

COPY target/spring-openproject-mcp-server*.jar app.jar
COPY target/classes/application-prod.properties application-prod.properties
COPY target/classes/META-INF/sbom/application.cdx.json application.cdx.json
COPY target/classes/META-INF/sbom/application.spdx.json application.spdx.json

RUN addgroup --system spring && \
    adduser --system --ingroup spring spring && \
    chown -R spring:spring /app

USER spring:spring
# ...

EXPOSE 8080

HEALTHCHECK --interval=5m --timeout=3s --start-period=20s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health/readiness | grep -q '"status":"UP"' || exit 1

# Command to run the application
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=prod -jar /app/app.jar"]

####################################
# --platform linux/amd64,linux/arm64