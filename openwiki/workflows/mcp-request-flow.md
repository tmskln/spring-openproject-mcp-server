---
type: workflow
title: MCP Request Flow
description: End-to-end flow from MCP client initialization through authentication filtering, tool/prompt dispatch, OpenProject API calls, and response delivery, covering both SSE and STREAMABLE protocols.
tags: [mcp, request-flow, authentication, openproject, sse, streamable, lifecycle]
verified:
  - by: openwiki/0.7.3
    at: 2026-11-04T19:49:05.158Z
sources:
  - id: openwiki-source-82c542ca96cbaac512149e7e
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/EnvOpenProjectConnectionResolver.java
  - id: openwiki-source-f46f7c36440cc3579b3f7d27
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/HttpHeaderOpenProjectConnectionResolver.java
  - id: openwiki-source-51034e397be644c1c6656eb0
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java
  - id: openwiki-source-e720a535b92dcb20db15fc95
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectAuthHeaderFactory.java
  - id: openwiki-source-c4ab00efd6a2e06b1c6235c7
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestClientFactory.java
  - id: openwiki-source-148dc7b9fcf7c6f829f368f3
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestOperations.java
  - id: openwiki-source-2001e664458f3b19d51bfab6
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectTemplateRenderer.java
  - id: openwiki-source-142642e34cf2be342272c586
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/security/McpBearerAuthenticationFilter.java
  - id: openwiki-source-6964ee234ef2765adb90b846
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidationCache.java
  - id: openwiki-source-cf7a926b8341fa826d55f76e
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidator.java
  - id: openwiki-source-55c59fb1360249a4d293fe36
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java
  - id: openwiki-source-664e3158e639953627819cca
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/SpringOpenprojectMcpServerApplication.java
  - id: openwiki-source-9ca3da2c174d17b004650f28
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/Common.java
  - id: openwiki-source-a93b61e7dbfd967f82424947
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/PriorityTools.java
  - id: openwiki-source-37ba579fec4adf35564fca68
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/ProjectTools.java
  - id: openwiki-source-6a41f21fa292cc615788a21b
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/RelationTools.java
  - id: openwiki-source-068122b02a47eaf28bc87205
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/TypesTools.java
  - id: openwiki-source-72b6712abe58306f7f03f39a
    resource: repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/WorkPackageTools.java
  - id: openwiki-source-966b12787695516ba5b32311
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpSecurityIntegrationTest.java
  - id: openwiki-source-fedd9a4e5dca22ccf60fefb8
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsHelper.java
generated: { by: "openwiki/0.7.3", at: "2026-11-09T18:33:59.081Z" }
---

# MCP Request Flow

This page traces the complete lifecycle of an MCP (Model Context Protocol) request in the OpenProject MCP server, from client initialization through authentication, tool/prompt dispatch, OpenProject API interaction, and response delivery.

## Protocol Overview

The server supports two MCP transport protocols, both served over HTTP at `/mcp`:

- **SSE (Server-Sent Events)**: The client first connects to `/sse` to receive an SSE stream, which includes a `Mcp-Session-Id` header. Subsequent JSON-RPC requests are POSTed to `/mcp` with that session ID in the `Mcp-Session-Id` header.
- **STREAMABLE**: All communication happens via HTTP POST to `/mcp` with session tracking handled through response headers (`Mcp-Session-Id`).

Both protocols use JSON-RPC 2.0 as the message format. The protocol version used is `2024-11-05` [[repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsHelper.java#L22-L23]].

The server is configured for STREAMABLE mode by default in tests via `spring.ai.mcp.server.protocol=STREAMABLE` [[repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpSecurityIntegrationTest.java#L39]].

## Authentication Flow

### Entry Point: Security Filter Chain

Spring Security configures two filter chains in `SecurityConfig` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java#L34-L84]]:

1. **Actuator chain** (`@Order(1)`): Matches `/actuator/**`. Requires HTTP Basic auth with `ACTUATOR_ADMIN` role for all endpoints except `/actuator/health` and `/actuator/health/*` which are permitted without auth [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java#L57-L63]].

2. **MCP chain** (`@Order(2)`): Matches `/mcp/**`, `/mcp`, `/error`, and `/sse`. Requires all requests to be authenticated via Bearer token. HTTP Basic is explicitly disabled for this chain [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java#L72-L82]].

The `McpBearerAuthenticationFilter` is injected into the MCP chain via `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java#L75]].

### Filter Registration

`McpBearerAuthenticationFilter` is a `@Component`, but Spring Boot's auto-registration is explicitly disabled by a `FilterRegistrationBean` with `setEnabled(false)` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/SecurityConfig.java#L46-L55]]. This ensures the filter only operates within the Spring Security chain, not as a global servlet filter.

### Bearer Token Validation

`McpBearerAuthenticationFilter.doFilterInternal()` processes every incoming request to `/mcp`, `/sse`, or their sub-paths [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/McpBearerAuthenticationFilter.java#L45-L47]]:

1. **Filter exclusion**: `shouldNotFilter()` skips requests whose URI does not match `/sse`, `/mcp`, or paths starting with `/mcp/` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/McpBearerAuthenticationFilter.java#L32-L35]].

2. **Async dispatch**: `shouldNotFilterAsyncDispatch()` returns `false` to ensure authentication runs on async dispatches (critical for SSE) [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/McpBearerAuthenticationFilter.java#L39-L41]].

3. **Token validation**: Calls `OpenProjectTokenValidator.validateCurrentRequestOrThrow()`:
   - `OpenProjectConnectionResolver` (default: `HttpHeaderOpenProjectConnectionResolver`) extracts the `Authorization: Bearer <token>` header and the `X-OpenProject-Base-Url` header (if `openproject.allow-header-base-url` is true) from the current request [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/HttpHeaderOpenProjectConnectionResolver.java#L30-L48]].
   - The token is SHA-256 hashed (combined with the base URL) to produce a cache key. The raw token is never stored in the cache [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidator.java#L41-L42]].
   - `OpenProjectTokenValidationCache.cachedCheckToken()` performs a Spring Cache lookup with `cacheNames = "auth-validation"` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidationCache.java#L16-L17]]. On cache miss, it calls `fetchUserFromOpenProject()` which invokes `OpenProjectApiClient.root()` to call `GET /api/v3` on OpenProject. A successful response (non-null `href`) proves the token is valid [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidator.java#L51-L60]].

4. **Security context**: On success, a `UsernamePasswordAuthenticationToken` with `ROLE_MCP_USER` authority is created and stored in `SecurityContextHolder`, then persisted via `RequestAttributeSecurityContextRepository` for async dispatch recovery [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/McpBearerAuthenticationFilter.java#L54-L60]].

5. **Failure**: On `UnauthorizedException`, the security context is cleared and a `401 Unauthorized` response with body `{"error":"unauthorized"}` is written [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/McpBearerAuthenticationFilter.java#L62-L66]].

### Test Stubbing

Tests use `McpSecurityIntegrationTest.McpHeaderAwareTokenValidatorTestConfig` which provides a `@Primary` `OpenProjectTokenValidator` bean that stubs `fetchUserFromOpenProject()` to accept only `VALID_TOKEN = "correctToken"` and reject all others [[repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpSecurityIntegrationTest.java#L62-L77]].

## Request Dispatch

### Initialization

The MCP client sends an `initialize` JSON-RPC request to `/mcp` containing:
- `protocolVersion`: `"2024-11-05"`
- `capabilities`: client capabilities
- `clientInfo`: name and version

On success, the server returns the initialization response and, in SSE mode, the `Mcp-Session-Id` header for subsequent requests [[repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsHelper.java#L58-L79]].

### Tool Dispatch

Tool methods are defined via `@McpTool` annotations on Spring `@Component` beans in the `tools` package:

| Tool Class | MCP Tools |
|---|---|
| `WorkPackageTools` | `workPackageList`, `workPackageShow`, `workPackageCreate`, `workPackageUpdate`, `workPackageUploadAttachment` |
| `PriorityTools` | `priorityList` |
| `ProjectTools` | `projectList` |
| `RelationTools` | `relationAllowedValues`, `relationList`, `relationAdd`, `relationDelete`, `relationAddParent`, `relationDeleteParent` |
| `TypesTools` | `typeList` |

Each tool method delegates to `OpenProjectApiClient` for the actual API call. The `@McpTool` annotation provides the tool name (derived from the method name), description, and `readOnlyHint` metadata for the MCP protocol [[repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/WorkPackageTools.java#L44-L104]][[repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/RelationTools.java#L34-L76]].

### Prompt Dispatch

Prompts are defined via `@McpPrompt` annotations:

| Prompt | Purpose |
|---|---|
| `openproject.workpackage.safe_edit` | Safe edit workflow with diff + confirmation, lockVersion handling, Markdown `/n` newline directive [[repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/WorkPackageTools.java#L106-L142]] |
| `openproject.project.summary` | Project summary generation [[repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/ProjectTools.java#L38-L47]] |

### Logging

`Common` class provides an `@McpLogging` handler that receives MCP logging notifications and logs them via SLF4J [[repo://src/main/java/de/tklein/tklab/openproject/mcp/tools/Common.java#L15-L19]].

## OpenProject API Interaction

### Client Factory

`OpenProjectRestClientFactory` creates a `RestClient` per request:
1. Resolves `OpenProjectConnection` (base URL + bearer token) via `OpenProjectConnectionResolver`
2. Converts the bearer token to an OpenProject-compatible `Authorization: Basic <base64(apikey:token)>` header via `OpenProjectAuthHeaderFactory` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectAuthHeaderFactory.java#L12-L17]]
3. Clones the Spring `RestClient.Builder` and sets the base URL and default header [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestClientFactory.java#L17-L23]]

### REST Operations

`OpenProjectRestOperations` wraps HTTP operations with HAL+JSON content negotiation:

- **GET**: `getJson(uriTemplate, ...)` — `Accept: application/hal+json` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestOperations.java#L33-L38]]
- **POST**: `postJson(uriTemplate, jsonBody, ...)` — `Content-Type: application/json`, `Accept: application/hal+json` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestOperations.java#L40-L46]]
- **PATCH**: `patchJson(uriTemplate, jsonBody, ...)` — same headers as POST [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestOperations.java#L48-L54]]
- **DELETE**: `delete(uriTemplate, ...)` — `Accept: application/hal+json`, returns bodiless entity [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestOperations.java#L56-L60]]
- **Multipart POST**: `postMultipart(uriTemplate, body, ...)` — `Content-Type: multipart/form-data` for attachment uploads [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestOperations.java#L62-L68]]

JSON bodies are constructed via FreeMarker templates (`OpenProjectTemplateRenderer`) that render PatchMap models (e.g., `create_work_package.ftl`, `update_parent_relation.ftl`) [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L71-L90]][[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectTemplateRenderer.java#L19-L26]].

### Work Package Lifecycle Operations

`OpenProjectApiClient` provides the core OpenProject API methods:

- **`workPackageList(projectId)`**: GET `/api/v3/projects/{projectId}/work_packages?pageSize=1000`, maps `_embedded.elements` via `WorkPackageMapper` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L57-L61]]
- **`workPackageShow(wpId)`**: GET `/api/v3/work_packages/{wpId}`, returns `null` on 404 [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L63-L70]]
- **`workPackageCreate(projectId, dto)`**: POST `/api/v3/projects/{projectId}/work_packages` with FreeMarker-rendered JSON, returns new work package ID [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L72-L84]]
- **`workPackageUpdate(wpId, dto)`**: PATCH `/api/v3/work_packages/{wpId}` with lockVersion, returns `true` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L86-L99]]
- **`workPackageUploadAttachment(wpId, fileName, content, contentType)`**: POST multipart with metadata + file parts [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L101-L106]]

### Relation Resolution

`listRelationsForWorkPackage()` assembles relations from four sources:
1. `_embedded.relations._embedded.elements` (explicit relations)
2. `_links.parent` (derived parent relation)
3. `_links.children[]` (derived children)
4. `_links.ancestors[]` (derived ancestors)

This multi-source approach is necessary because OpenProject exposes parent/child/ancestor relationships through `_links` rather than the `_embedded.relations` structure [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L120-L167]].

## Response Delivery

### STREAMABLE Protocol

The server responds with JSON-RPC responses over HTTP. Session state is managed via the `Mcp-Session-Id` response header. Each request is authenticated independently via the `Authorization` header.

### SSE Protocol

The server streams Server-Sent Events to the client. JSON-RPC responses are delivered as SSE `data:` events. The `Mcp-Session-Id` header in the initial SSE connection is used for subsequent request correlation.

### Error Handling

- **Authentication failure**: `401 Unauthorized` with `{"error":"unauthorized"}` [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/McpBearerAuthenticationFilter.java#L62-L66]]
- **Missing OpenProject URL/token**: `UnauthorizedException` with descriptive message [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidator.java#L35-L40]]
- **Invalid OpenProject token**: `UnauthorizedException` with HTTP status from OpenProject's response [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidator.java#L58-L60]]
- **404 on work package show**: Returns `null` rather than propagating the exception [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L66-L68]]
- **Relation delete failure**: Logs warning and returns `false` rather than throwing [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L179-L185]]

## Connection Resolver Switching

`OpenProjectConnectionResolver` is an interface with two implementations:

1. **`HttpHeaderOpenProjectConnectionResolver`** (active `@Component`): Extracts `X-OpenProject-Base-Url` and `Authorization` headers from the current `HttpServletRequest`. Falls back to `openproject.url` config for the base URL. The `X-OpenProject-Base-Url` header usage is controlled by `openproject.allow-header-base-url` (default: `false`) [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/HttpHeaderOpenProjectConnectionResolver.java#L22-L54]].

2. **`EnvOpenProjectConnectionResolver`** (commented out `@Component`): Reads `OPENPROJECT_BASE_URL` and `OPENPROJECT_BEARER_TOKEN` from environment variables. Provided as a future alternative for STDIO transport where HTTP headers are not available [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/EnvOpenProjectConnectionResolver.java#L10-L18]].

## Tests

Integration tests in the `tools` package verify the complete request flow:

- **`McpSecurityIntegrationTest`**: Parameterized tests covering initialization auth, tools/list auth, prompts/list auth, and ping auth — each with valid token, wrong token, and missing token scenarios [[repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpSecurityIntegrationTest.java#L43-L136]].

- **`McpToolsHelper`**: Test utility that constructs raw HTTP requests with the correct MCP headers (`MCP-Protocol-Version`, `Mcp-Session-Id`, `Accept: text/event-stream, application/json`), sends them to a random-port server, and parses SSE or JSON responses [[repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsHelper.java#L33-L310]].

- **`McpPingIntegrationTest`**, **`McpPromptsIntegrationTest`**, **`McpToolsListIntegrationTest`**: Protocol-level tests for ping, prompts, and tools endpoints respectively.

- **`WorkPackageToolsTest`**, **`RelationToolsTest`**, etc.: Unit tests for individual tool classes.

## Mermaid Diagram

<!-- openwiki: mermaid parse failed and this diagram was converted to a text fence so it does not break rendering. Fix the diagram source and restore the mermaid fence. Parser error: Heuristic: an unescaped angle bracket inside a label breaks rendering; rephrase the label. -->
```text
sequenceDiagram
    participant Client as MCP Client
    participant Server as MCP Server
    participant Filter as McpBearerAuthFilter
    participant Validator as OpenProjectTokenValidator
    participant Cache as TokenValidationCache
    participant OP as OpenProject API

    Client->>Server: POST /mcp (initialize)
    Server->>Filter: doFilterInternal()
    Filter->>Validator: validateCurrentRequestOrThrow()
    Validator->>Validator: resolve OpenProjectConnection
    Validator->>Validator: SHA-256 hash(token + baseUrl)
    Validator->>Cache: cachedCheckToken(key, supplier)
    alt Cache hit
        Cache-->>Validator: ValidatedOpenProjectUser
    else Cache miss
        Validator->>OP: GET /api/v3 (Authorization: Basic <apikey:token>)
        OP-->>Validator: UserDto (href, name)
        Validator->>Cache: store(key, user)
        Cache-->>Validator: ValidatedOpenProjectUser
    end
    Validator-->>Filter: ValidatedOpenProjectUser
    Filter->>Filter: Set SecurityContext (ROLE_MCP_USER)
    Filter-->>Server: continue chain
    Server-->>Client: JSON-RPC response + Mcp-Session-Id

    Client->>Server: POST /mcp (tools/list)
    Server->>Filter: doFilterInternal()
    Filter->>Validator: validateCurrentRequestOrThrow()
    Validator->>Cache: cachedCheckToken(key)
    Cache-->>Validator: ValidatedOpenProjectUser
    Validator-->>Filter: ValidatedOpenProjectUser
    Filter-->>Server: continue chain
    Server->>Server: dispatch to WorkPackageTools / ProjectTools / etc.
    Server->>OP: GET /api/v3/projects/{id}/work_packages
    OP-->>Server: HAL+JSON WorkPackage[]
    Server-->>Client: JSON-RPC response

    Client->>Server: POST /mcp (tools/call)
    Server->>Filter: doFilterInternal()
    Filter->>Validator: validateCurrentRequestOrThrow()
    Validator->>Cache: cachedCheckToken(key)
    Cache-->>Validator: ValidatedOpenProjectUser
    Validator-->>Filter: ValidatedOpenProjectUser
    Filter-->>Server: continue chain
    Server->>Server: dispatch to specific tool method
    Server->>OP: POST/PATCH /api/v3/work_packages/{id}
    OP-->>Server: HAL+JSON response
    Server-->>Client: JSON-RPC response
```

## Key Invariants

1. **Token caching**: Every validated token is cached by its SHA-256 hash. The raw token is never stored in the cache, preventing token leakage in cache stores [[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidator.java#L41-L42]][[repo://src/main/java/de/tklein/tklab/openproject/mcp/security/OpenProjectTokenValidationCache.java#L16-L17]].

2. **Bearer-to-Basic conversion**: The MCP-level `Authorization: Bearer <token>` header is converted to `Authorization: Basic <base64(apikey:<token>)>` for OpenProject API calls [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectAuthHeaderFactory.java#L12-L17]].

3. **Per-request RestClient**: Each OpenProject API call gets a fresh `RestClient` instance with the current connection's base URL and auth header, preventing stale configuration [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestClientFactory.java#L17-L23]].

4. **HAL+JSON content negotiation**: All OpenProject API calls use `Accept: application/hal+json` and parse `_embedded.elements` arrays for collection responses [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestOperations.java#L33-L38]][[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectRestOperations.java#L93-L101]].

5. **PatchMap models**: OpenProject API JSON bodies are constructed via FreeMarker templates from `PatchMap` models, enabling selective field updates (PATCH semantics) rather than full object serialization [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L71-L90]].

6. **Multi-source relation resolution**: Work package relations are assembled from both `_embedded.relations` and `_links` (parent, children, ancestors), requiring a combined approach not all MCP clients may anticipate [[repo://src/main/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClient.java#L120-L167]].
