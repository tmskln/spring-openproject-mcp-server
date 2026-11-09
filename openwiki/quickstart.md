---
type: "Reference"
title: "Quickstart"
openwiki_generated: true
sources:
  - id: openwiki-source-7f1ca00753e95588bc8efa39
    resource: repo://docker/Dockerfile
  - id: openwiki-source-2355f81d7cf522f8dbdaabd4
    resource: repo://pom.xml
  - id: openwiki-source-23775c3de52f3ab95a13cb8b
    resource: repo://README.md
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
  - id: openwiki-source-e7254eded120c6c1ee31556b
    resource: repo://src/main/resources/templates/create_work_package.ftl
generated: { by: "openwiki/0.7.3", at: "2026-11-04T19:49:05.158Z" }
---


## Spring OpenProject MCP Server

This project implements an [MCP](https://modelcontextprotocol.io/) server in Java that proxies OpenProject's REST API, enabling AI agents to manage work packages, projects, priorities, types, and relations through standardized tool and prompt calls. The server authenticates each request via a user-provided OpenProject bearer token (never stored in the container), resolves the target OpenProject URL, and delegates REST calls through a `RestClient`-based client with HAL+JSON parsing and Freemarker payload rendering.

**Key facts**

- **Stack**: Spring Boot 4.1.1 · Spring AI MCP Server 2.0.1 · Java 25 · Tomcat 11.0.26
- **Ports**: `8080` (MCP / app) · `8081` (Actuator health)
- **Protocol**: SSE (default) or STREAMABLE via `spring.ai.mcp.server.protocol`
- **Profiles**: `dev`, `local`, `prod`, `test`
- **Compatibility**: OpenProject 14, 15, 16.6, 17.7
- **Auth**: Bearer token in `Authorization` header; optional multi-tenant URL via `X-OpenProject-Base-Url` header
- **Env vars**: `OPENPROJECT_URL`, `OPENPROJECT_ALLOW_HEADER_BASE_URL`

## Wiki Routing Map

### Architecture

| Page | What it covers |
|------|----------------|
| [System Overview](architecture/overview.md) | High-level component diagram and boundaries |
| [MCP Protocol Layer](architecture/mcp-protocol.md) | Tool/prompt registration, SSE vs STREAMABLE transport |
| [OpenProject REST Client](architecture/openproject-client.md) | `OpenProjectApiClient`, connection resolver, HAL+JSON parsing, Freemarker templates |
| [Security Architecture](architecture/security.md) | Bearer validation, Spring Security filter chains, Caffeine token cache |

### Concepts

| Page | What it covers |
|------|----------------|
| [Configuration and Profiles](concepts/configuration.md) | `application-{profile}.properties`, profile selection, MCP protocol config |
| [DTOs and Mappers](concepts/dtos-and-mappers.md) | WorkPackage, Project, Priority, Type, Relation, User DTOs and MapStruct mappers |

### Workflows

| Page | What it covers |
|------|----------------|
| [MCP Request Flow](workflows/mcp-request-flow.md) | End-to-end: initialize → auth → tool dispatch → OpenProject API → response |
| [Work Package Lifecycle](workflows/work-package-lifecycle.md) | Create, update, safe-edit prompt, lockVersion optimistic locking, attachment upload |

### Operations

| Page | What it covers |
|------|----------------|
| [Deployment](operations/deployment.md) | Docker image, multi-platform build, SBOM, health check, env vars |

### Testing

| Page | What it covers |
|------|----------------|
| [Integration Testing](testing/integration-tests.md) | `SpringBootTest` with `RANDOM_PORT`, MCP test helpers, Docker Compose OpenProject, unit tests |

## Getting Started Locally

1. Set `OPENPROJECT_URL` to your OpenProject instance.
2. Start the container:

   ```bash
   docker run -d -p 0.0.0.0:8080:8080 \
     -e OPENPROJECT_URL=https://your-openproject-url \
     --tmpfs /tmp \
     docker.io/tmskln/spring-openproject-mcp-server:latest
   ```

3. Configure your MCP client (e.g. LM Studio) with the SSE endpoint and your OpenProject API token:

   ```json
   {
     "mcpServers": {
       "openproject-mcp": {
         "url": "http://127.0.0.1:8080/sse",
         "headers": {
           "Authorization": "Bearer {yourToken}"
         }
       }
     }
   }
   ```

4. Prompt the MCP tools, e.g. `"List all projects in OpenProject using mcp/openproject-mcp."`

For multi-tenant setups, enable header-based URL resolution:

```bash
-e OPENPROJECT_ALLOW_HEADER_BASE_URL=true
```

Then include `X-OpenProject-Base-Url` in each MCP client request header.

## Quick Reference

| Resource | Path |
|----------|------|
| Source root | `repo://src/main/java/de/tklein/tklab/openproject/mcp/` |
| Tools | `repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/` |
| Security | `repo://src/main/java/de/tklein/tklab/openproject/mcp/security/` |
| Client | `repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/` |
| DTOs | `repo://src/main/java/de/tklein/tklab/openproject/mcp/dto/` |
| Mappers | `repo://src/main/java/de/tklein/tklab/openproject/mcp/mapper/` |
| Dockerfile | `repo://docker/Dockerfile` |
| Prod config | `repo://src/main/resources/application-prod.properties` |
| Dev config | `repo://src/main/resources/application-dev.properties` |
| Freemarker templates | `repo://src/main/resources/templates/` |
