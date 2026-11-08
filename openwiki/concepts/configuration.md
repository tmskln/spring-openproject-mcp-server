---
type: configuration and profiles
title: Configuration and Profiles
description: Spring Boot profile system (dev, local, prod, test) for the OpenProject MCP server, including application properties, environment variables, MCP server protocol selection (SSE vs STREAMABLE), and deployment-time profile activation.
tags: [spring-boot, profiles, configuration, mcp-server, sse, streamable, deployment]
verified:
  - by: openwiki/0.7.3
    at: 2026-11-04T19:49:05.158Z
sources:
  - id: openwiki-source-7f1ca00753e95588bc8efa39
    resource: repo://docker/Dockerfile
  - id: openwiki-source-2355f81d7cf522f8dbdaabd4
    resource: repo://pom.xml
  - id: openwiki-source-f46f7c36440cc3579b3f7d27
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/HttpHeaderOpenProjectConnectionResolver.java
  - id: openwiki-source-55c59fb1360249a4d293fe36
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java
  - id: openwiki-source-8410eeba5382dc4b1424b526
    resource: repo://src/main/resources/application-dev.properties
  - id: openwiki-source-c8948fffa3abb80f9c097fd5
    resource: repo://src/main/resources/application-local.properties
  - id: openwiki-source-34b040304a4e07efc4f92367
    resource: repo://src/main/resources/application-prod.properties
  - id: openwiki-source-ece093ad10b91f3da0b7e34f
    resource: repo://src/main/resources/application.properties
  - id: openwiki-source-1aebb6e3b834fbb2a0bfe08a
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpPingIntegrationTest.java
  - id: openwiki-source-19fd6b1f4ed7ed5a26a1cbcc
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpPromptsIntegrationTest.java
  - id: openwiki-source-966b12787695516ba5b32311
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpSecurityIntegrationTest.java
  - id: openwiki-source-e64dc33cbf551deff2abfea4
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsListIntegrationTest.java
  - id: openwiki-source-96b9468c207e0c94df548581
    resource: repo://src/test/resources/application-test.properties
generated: { by: "openwiki/0.7.3", at: "2026-11-04T19:49:05.158Z" }
---

This page describes the configuration model for the OpenProject MCP Server, a Spring Boot application built on Spring AI's MCP server starter. Profiles control environment-specific behavior such as logging levels, OpenProject connection targets, health probes, and the MCP transport protocol.

## Profile Overview

The project defines five Spring Boot profile property files:

| Profile | File | Purpose |
|---------|------|---------|
| `base` (no profile) | `repo://src/main/resources/application.properties` | Default configuration shared by all environments |
| `dev` | `repo://src/main/resources/application-dev.properties` | Developer workstation debugging |
| `local` | `repo://src/main/resources/application-local.properties` | Local integration against a specific OpenProject instance |
| `prod` | `repo://src/main/resources/application-prod.properties` | Production deployment (default for Docker) |
| `test` | `repo://src/test/resources/application-test.properties` | Container-based integration tests |

Profiles are **not** activated by `spring.profiles.active` inside any properties file. Instead, activation is controlled externally:

- **Docker deployment** activates `prod` via JVM argument: `java -Dspring.profiles.active=prod -jar /app/app.jar` ([repo://docker/Dockerfile#L29](repo://docker/Dockerfile#L29))
- **IDE / local runs** activate a profile via IDE run configuration, command-line `--spring.profiles.active=dev`, or the `SPRING_PROFILES_ACTIVE` environment variable.
- **Tests** use `@SpringBootTest` with `properties = { ... }` to override individual settings rather than activating a profile file.

The Maven build excludes all `application-*.properties` from the JAR ([repo://pom.xml#L278-L281](repo://pom.xml#L278-L281)). At Docker image build time, `application-prod.properties` is copied separately into the image root ([repo://docker/Dockerfile#L13](repo://docker/Dockerfile#L13)), so the production profile's overrides are available at runtime.

## Base Configuration (No Profile)

`application.properties` establishes the application's defaults:

- **Application identity**: `spring.application.name=spring-openproject-mcp-server`
- **Server**: HTTP on `0.0.0.0:8080`; management (actuator) on port `8081`
- **Caching**: Caffeine cache named `auth-validation`, evicting after 60 minutes of inactivity with stats recording ([repo://src/main/resources/application.properties#L6-L8](repo://src/main/resources/application.properties#L6-L8))
- **OTLP observability**: Tracing, logging, and metrics export are all **disabled** by default ([repo://src/main/resources/application.properties#L10-L12](repo://src/main/resources/application.properties#L10-L12))
- **MCP server** ([repo://src/main/resources/application.properties#L14-L20](repo://src/main/resources/application.properties#L14-L20)):
  - Enabled (`spring.ai.mcp.server.enabled=true`)
  - Name: `openproject-mcp-server`, version `1.2.0`
  - **Protocol: `sse`** (Server-Sent Events) — the default transport
  - Tool capability enabled (`spring.ai.mcp.server.capabilities.tool=true`)
  - Type: `sync` (synchronous server)
  - Annotation scanner enabled
- **OpenProject connection**: Default URL is `https://localhost`; header-based base URL override is **disabled** (`openproject.allow-header-base-url=false`)
- **Actuator security**: Admin username `admin` with a placeholder bcrypt password hash; endpoints are **disabled** by default (`endpoints.enabled=false`)

## Profile-Specific Overrides

### `dev` Profile

`application-dev.properties` ([repo://src/main/resources/application-dev.properties](repo://src/main/resources/application-dev.properties)):

- Sets `logging.level.org.springframework.ai=DEBUG` and `logging.level.io.modelcontextprotocol=DEBUG` for verbose AI/MCP debugging.
- Disables Spring DevTools restart (`spring.devtools.restart.enabled=false`) to avoid interference with the MCP server's long-running SSE connections.

This profile is intended for developers running the app locally in an IDE who need visibility into Spring AI and MCP protocol traffic.

### `local` Profile

`application-local.properties` ([repo://src/main/resources/application-local.properties](repo://src/main/resources/application-local.properties)):

- Overrides `openproject.url=https://pro.t-klein.de` to target a specific OpenProject instance.
- Enables `TRACE` logging for the OpenProject client package (`de.tklein.tklab.openproject.mcp.openproject.client`) for deep HTTP debugging.

This profile is for a developer's personal integration environment pointing at a known OpenProject deployment.

### `prod` Profile

`application-prod.properties` ([repo://src/main/resources/application-prod.properties](repo://src/main/resources/application-prod.properties)):

- Reduces logging: `ERROR` for Spring framework, `INFO` for the application package.
- Hides the Spring banner (`spring.main.banner-mode=off`).
- Exposes only the `/actuator/health` endpoint at base path `/actuator`.
- Disables detailed health output (`show-details=never`, `show-components=never`).
- Enables Kubernetes-style readiness/liveness probes (`management.endpoint.health.probes.enabled=true`).
- Disables disk space and SSL health checks (`management.health.diskspace.enabled=false`, `management.health.ssl.enabled=false`).

This profile is the **only** one bundled into the Docker image and is activated automatically at container start.

### `test` Profile

`application-test.properties` ([repo://src/test/resources/application-test.properties](repo://src/test/resources/application-test.properties)):

- Sets the OpenProject container image tag to `17.7.2` (`openproject.container.tag`).
- Disables automatic container shutdown (`openproject.container.shutdown=false`).
- Configures the container port to `18088` (`openproject.container.port`).
- Explicitly sets `openproject.allow-header-base-url=false`.

This profile supports integration tests that spin up a real OpenProject container to validate end-to-end MCP tool behavior.

## MCP Server Protocol Selection

The MCP server supports two transport protocols:

| Protocol | Property | Description |
|----------|----------|-------------|
| `sse` (default) | `spring.ai.mcp.server.protocol=sse` | Server-Sent Events. The server maintains a persistent SSE connection to stream messages to the client. Default in production. |
| `STREAMABLE` | `spring.ai.mcp.server.protocol=STREAMABLE` | HTTP-based streamable transport. Each request is a discrete HTTP call with optional continuation. No persistent connection. |

The base configuration defaults to `sse` ([repo://src/main/resources/application.properties#L18](repo://src/main/resources/application.properties#L18)). The `prod` profile does not override this, so production deployments use SSE.

Integration tests override the protocol to `STREAMABLE` at the test level ([repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpPingIntegrationTest.java#L22](repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpPingIntegrationTest.java#L22), [repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpSecurityIntegrationTest.java#L28](repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpSecurityIntegrationTest.java#L28), [repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsListIntegrationTest.java#L29](repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsListIntegrationTest.java#L29), [repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpPromptsIntegrationTest.java#L26](repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpPromptsIntegrationTest.java#L26)). The test comments explain that `STREAMABLE` is preferred in tests because it avoids leaving an open SSE stream during test teardown.

## Configuration Injection Points

Configuration values are consumed by the application through Spring's `@Value` injection:

- **`openproject.allow-header-base-url`** — controls whether the `X-OpenProject-Base-Url` HTTP header is trusted to override the base URL. Injected in `HttpHeaderOpenProjectConnectionResolver` ([repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/HttpHeaderOpenProjectConnectionResolver.java#L24](repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/HttpHeaderOpenProjectConnectionResolver.java#L24)).
- **`actuator.security.admin.username`** and **`actuator.security.admin.password-hash`** — secure the actuator endpoint. Injected in `SecurityConfig` ([repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java#L32-L33](repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java#L32-L33)).

## Deployment Configuration

The Docker image ([repo://docker/Dockerfile](repo://docker/Dockerfile)) packages the application as follows:

1. Base image: Eclipse Temurin JRE (JDK 25).
2. Copies the application JAR as `app.jar`.
3. Copies `application-prod.properties` to the image root (not inside `BOOT-INF/classes`).
4. Runs as a non-root `spring` user.
5. Activates the `prod` profile via `-Dspring.profiles.active=prod`.
6. Exposes port `8080` (HTTP) and `8081` (actuator).
7. Health check queries the actuator readiness endpoint on port `8081`.

The production profile's health probe configuration (`management.endpoint.health.probes.enabled=true`) enables Kubernetes-style `/actuator/health/liveness` and `/actuator/health/readiness` endpoints, which the Docker `HEALTHCHECK` directive queries.
