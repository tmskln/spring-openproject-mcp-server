---
type: testing-strategy
title: Integration Testing
description: Test strategy for the OpenProject MCP server, covering Spring Boot random-port integration tests for MCP protocol methods (ping, tools/list, prompts/list), security stub configurations for authentication testing, Docker Compose lifecycle management for a real OpenProject container, and unit tests for the PatchMap utility.
tags: [integration-testing, mcp-protocol, security-testing, docker-compose, spring-boot-test]
verified:
  - by: openwiki/0.7.3
    at: 2026-11-04T19:49:05.158Z
sources:
  - id: openwiki-source-98d2be7588b0c75ac09744ba
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClientIntegrationTest.java
  - id: openwiki-source-5e4e10129e139da58ea99cc2
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/TestConstants.java
  - id: openwiki-source-dadcb937c23302cfdf71ad77
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpNoAuthTestSecurityConfig.java
  - id: openwiki-source-1aebb6e3b834fbb2a0bfe08a
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpPingIntegrationTest.java
  - id: openwiki-source-19fd6b1f4ed7ed5a26a1cbcc
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpPromptsIntegrationTest.java
  - id: openwiki-source-966b12787695516ba5b32311
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpSecurityIntegrationTest.java
  - id: openwiki-source-fedd9a4e5dca22ccf60fefb8
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsHelper.java
  - id: openwiki-source-e64dc33cbf551deff2abfea4
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsListIntegrationTest.java
  - id: openwiki-source-8fa6c69caadd54457dbd3fa9
    resource: repo://src/test/java/de/tklein/tklab/openproject/mcp/util/PatchMapTest.java
  - id: openwiki-source-96b9468c207e0c94df548581
    resource: repo://src/test/resources/application-test.properties
  - id: openwiki-source-b11dcb0a8c9e2f96725537fc
    resource: repo://src/test/resources/openproject/docker-compose.yml
generated: { by: "openwiki/0.7.3", at: "2026-11-04T19:49:05.158Z" }
---

## Overview

The project uses two tiers of testing: **unit tests** for utility classes (e.g., `PatchMap`) and **integration tests** for the MCP protocol layer and the `OpenProjectApiClient` against a real OpenProject instance. Integration tests are driven by Spring Boot's `@SpringBootTest` with `RANDOM_PORT` for local MCP endpoint tests and by an externally managed Docker Compose container for remote API tests. Test discovery is controlled by JUnit 5 tags (`integration_local`, `integration_remote`, `security`).

## Test Tagging and Discovery

Test constants are centralized in [TestConstants](repo://src/test/java/de/tklein/tklab/openproject/mcp/TestConstants.java):

- `TAG_INTEGRATION_TEST_LOCAL` (`"integration_local"`) — local MCP protocol tests that start an embedded server.
- `TAG_INTEGRATION_TEST_REMOTE` (`"integration_remote"`) — tests that require a real OpenProject instance.
- `TAG_SECURITY` (`"security"`) — marks security-specific parameterized tests.

These tags are applied via `@Tag(...)` on test classes and individual methods, enabling selective test execution via Maven Surefire/Failsafe filters or IDE run configurations.

## Local MCP Protocol Tests

Tests in the `tools` package exercise the MCP server over HTTP against an embedded Spring Boot application. All use `SpringBootTest.WebEnvironment.RANDOM_PORT` to avoid port conflicts.

### Test Helper: `McpToolsHelper`

[McpToolsHelper](repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpToolsHelper.java) is a static utility that encapsulates the raw MCP protocol interaction:

- **`initializeAndGetSessionId(port)`** — Sends an MCP `initialize` JSON-RPC request to `/mcp`, asserts a 200 response, extracts the `Mcp-Session-Id` header, and returns the session ID for subsequent calls.
- **`initializeMcp(port, extraHeaders)`** — Builds and sends the raw `initialize` request with protocol version `2024-11-05`, `Accept: text/event-stream, application/json`, and the `MCP-Protocol-Version` header.
- **`callToolsList(port, sessionId)` / `callPromptsList(port, sessionId)`** — Sends `tools/list` and `prompts/list` JSON-RPC requests to `/mcp` with the session ID, parses the SSE-wrapped JSON-RPC response via `parseJsonRpcFromSse()`.
- **`callPing(port, sessionId)`** — Sends a `ping` JSON-RPC request and returns the parsed `JsonNode`.
- **`callToolsListRequest(port, sessionId, extraHeaders, expectStatus)`** — Overload that accepts an expected status code; when the expected status is not 200, it discards the response body and returns `null` after asserting the status.
- A shared `HttpClient` instance with a 5-second connect timeout is reused across all calls.

The helper parses SSE-delimited JSON-RPC responses by stripping the SSE framing, enabling assertions on the raw JSON-RPC structure (e.g., `jsonrpc` field, `id`, `error` absence).

### `McpPingIntegrationTest`

Verifies the MCP `ping` method returns valid JSON-RPC 2.0:

- Asserts `jsonrpc == "2.0"`, presence of `id`, and absence of `error`.
- Asserts `result` is a JSON object (empty result for ping).
- Uses `STREAMABLE` protocol via `spring.ai.mcp.server.protocol=STREAMABLE` to avoid keeping an SSE stream open during the test.
- Imports `McpNoAuthTestSecurityConfig` to bypass authentication for this basic protocol test.

### `McpToolsListIntegrationTest`

Verifies the MCP `tools/list` method:

- Asserts JSON-RPC 2.0 compliance (jsonrpc, id, no error).
- Asserts all 14 expected tools are present by name: `priorityList`, `projectList`, `relationAdd`, `relationAddParent`, `relationAllowedValues`, `relationDelete`, `relationDeleteParent`, `relationList`, `typeList`, `workPackageCreate`, `workPackageList`, `workPackageShow`, `workPackageUpdate`, `workPackageUploadAttachment`.
- Asserts every tool has a non-blank `name`, `description`, and an `inputSchema` of type `object` or empty.
- Uses JsonPath (`com.jayway.jsonpath:json-path`) to assert tool names can be extracted from the raw JSON response and that no names are blank.
- Concrete schema validation for `relationAdd`: checks `required` fields (`workPackageId`, `otherWorkPackageId`, `relationType`) and field-level descriptions in `inputSchema.properties`.

### `McpPromptsIntegrationTest`

Verifies the MCP `prompts/list` method:

- Asserts JSON-RPC 2.0 compliance.
- Asserts the response contains the expected prompts from the application: `openproject.workpackage.safe_edit` and `openproject.project.summary`.
- Asserts every prompt has a non-blank `name` and `description`.
- Parameterized test verifies specific prompt name/description pairs.

### `McpSecurityIntegrationTest`

Tests MCP authentication at the protocol level using parameterized tests (`@CsvSource`). This test class **does not** import `McpNoAuthTestSecurityConfig`, so real Spring Security applies.

**Authentication stub**: The inner `McpHeaderAwareTokenValidatorTestConfig` class provides a `@Primary` `OpenProjectTokenValidator` bean that overrides `fetchUserFromOpenProject()`:

- If the bearer token equals `"correctToken"`, it returns a fake `UserDto`.
- Otherwise, it throws `UnauthorizedException`.

**Tested scenarios** (parameterized with `correctToken→200`, `wrongToken→401`, `empty→401`, `null→401`):

- **`mcpInitializeAuth`** — The `initialize` request must include a valid `Authorization: Bearer <token>` header; without it or with a wrong token, the server returns 401.
- **`mcpToolsRequestWithoutInitializeAuth`** — `tools/list` sent with a `fakeSessionId` (never initialized) returns 401 regardless of the token.
- **`mcpToolsRequestWithCorrectMCPSession`** — After a valid `initialize`, subsequent `tools/list` requests accept both the correct token and any token (because the session was already validated during initialize).
- **`mcpPingRequestWithoutInitializeAuth` / `mcpPingRequestWithCorrectMcpSession`** — Same pattern for the `ping` method.
- **`mcpPromptsRequestWithCorrectMcpSession` / `mcpPromptsRequestWithoutInitializeAuth`** — Same pattern for the `prompts/list` method.

These tests confirm that: (1) authentication is enforced on `initialize`, (2) the session binding is correct, and (3) unauthorized requests to any MCP method are rejected with 401.

## Security Stub Configuration

[McpNoAuthTestSecurityConfig](repo://src/test/java/de/tklein/tklab/openproject/mcp/tools/McpNoAuthTestSecurityConfig.java) is a `@TestConfiguration` that overrides the production `SecurityFilterChain` for MCP endpoints:

```java
@Bean @Order(0)
SecurityFilterChain testMcpPermitAll(HttpSecurity http) {
    http
        .securityMatcher("/mcp", "/mcp/**", "/sse")
        .authorizeHttpRequests(reg -> reg.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);
    return http.build();
}
```

- **Order 0** ensures it takes priority over the production MCP filter chain (order 2).
- It matches `/mcp`, `/mcp/**`, and `/sse` endpoints and permits all requests.
- CSRF and HTTP Basic are disabled to avoid interference.
- Imported via `@Import(McpNoAuthTestSecurityConfig.class)` on `McpPingIntegrationTest`, `McpToolsListIntegrationTest`, and `McpPromptsIntegrationTest`.

## Remote Integration Test: `OpenProjectApiClient`

[OpenProjectApiClientIntegrationTest](repo://src/test/java/de/tklein/tklab/openproject/mcp/openproject/client/OpenProjectApiClientIntegrationTest.java) is the heavyweight integration test that spins up a real OpenProject Docker container and exercises the full `OpenProjectApiClient` against it.

### Container Lifecycle

The test manages the OpenProject container lifecycle through an `ApplicationContextInitializer`:

1. **`OpenProjectInitializer.initialize()`** — Reads `openproject.container.tag`, `openproject.container.port`, and `openproject.container.shutdown` from the Spring environment. Calls `ensureStarted()` which:
   - Runs `docker compose -f src/test/resources/openproject/docker-compose.yml up -d --pull always` with environment variables for image tag, port, and hostname.
   - Polls the container's health endpoint (`/api/v3`) with a 5-minute timeout, retrying every 3 seconds.
   - Generates an admin API key by executing a Rails runner script inside the container (`bundle exec rails runner`) that finds the admin user and extracts the plain API token.
   - Applies `openproject.url=<baseUrl>` as a test property via `TestPropertyValues`.

2. **`afterAll()`** — If `openproject.container.shutdown=true` (the default), runs `docker compose down -v` to tear down the container and volumes.

3. **`ensureStarted()`** is `synchronized` to prevent concurrent test threads from starting multiple containers.

### TestAuthConfig Stub

A `@TestConfiguration` inner class (`TestAuthConfig`) provides a `@Primary` `OpenProjectConnectionResolver` bean that returns a hardcoded `OpenProjectConnection` with the container's base URL and generated API key. This bypasses the production connection resolver so the `OpenProjectApiClient` always talks to the test container regardless of application properties.

### Test Setup (`initIds`)

After the container is ready:

1. Calls `client.root()` to verify the API key works and the connection is valid.
2. Finds the `"Demo project"` from `client.projectList()` (seeded via `OPENPROJECT_SEED_DUMMY_DATA=true`).
3. Caches `demoProjectId`, `demoTypeId`, and `demoPriorityId` for use in individual tests.

### Test Cases

| Test | What It Verifies |
|------|-----------------|
| `root_returnsAuthenticatedUser` | `client.root()` returns a non-null `UserDto` with a non-blank `href`. |
| `projectList_containsDemoProject` | `client.projectList()` includes a project named `"Demo project"`. |
| `workPackageShow_withNonExistentWpId` | `client.workPackageShow(Integer.MAX_VALUE)` returns `null` (404 → null contract). |
| `relationDelete_withNonExistentRelationId_returnsFalse` | `client.relationDelete(Integer.MAX_VALUE)` returns `false` (not an exception). |
| `relationAdd_withNonExistentWorkPackageId_returnsFalse` | Adding a relation with a non-existent work package ID throws `HttpClientErrorException` with status 422. |
| `workPackageList_withNonExistentProjectId_throws404Exception` | Listing work packages for a non-existent project throws `RestClientResponseException` with status 404. |
| `priorityList_returnsNonEmptyList_withIdsAndNames` | `client.priorityList()` returns non-empty entries with valid IDs and names. |
| `typeList_forDemoProject_returnsNonEmptyList_withIdsAndNames` | `client.typeList(demoProjectId)` returns valid type entries. |
| `workPackageCrud_and_attachments_and_relations_areFunctional` | Full end-to-end CRUD: create two work packages, list, show, update subject (with lockVersion), upload attachment, add relation, list relations, delete relation, add/delete parent relation, verify all succeed. |

### Docker Compose Configuration

[src/test/resources/openproject/docker-compose.yml](repo://src/test/resources/openproject/docker-compose.yml) defines the test OpenProject service:

- **Image**: `openproject/openproject:${OPENPROJECT_IMAGE_TAG}` (tag overridden by `application-test.properties`, default `17.7.2`).
- **Port mapping**: `${OPENPROJECT_PORT:-8088}:80`.
- **Environment**: `OPENPROJECT_HOST__NAME`, `OPENPROJECT_HTTPS=false`, `OPENPROJECT_SEED_DUMMY_DATA=true`, `OPENPROJECT_EMAIL__DELIVERY__METHOD=smtp`, `OPENPROJECT_DISABLE__SECRET_KEY_BASE__CHECK=true`.
- **Health check**: `wget -qO- http://localhost/api/v3` with 10s interval, 5s timeout, 60 retries.

### Test Properties

[src/test/resources/application-test.properties](repo://src/test/resources/application-test.properties) provides:

- `openproject.container.tag=17.7.2` — Docker image tag.
- `openproject.container.shutdown=false` — Whether to tear down the container after tests.
- `openproject.container.port=18088` — Host port for the container.
- `openproject.allow-header-base-url=false` — Disables header-based URL override in production.

## Unit Test: `PatchMap`

[PatchMapTest](repo://src/test/java/de/tklein/tklab/openproject/mcp/util/PatchMapTest.java) exercises the `PatchMap` utility class (used by `OpenProjectApiClient` for building JSON patch bodies):

- **`of(Nullable.DROP_NULL_VALUES, ...)`** — Filters out entries with `null` values; empty strings, `0`, and `false` are preserved.
- **`of(Nullable.ALLOW_NULL_VALUES, ...)`** — Includes `null` values in the result map.
- **`of(Nullable.DROP_NULL_VALUES)`** with no pairs — Returns an empty map.
- **Odd argument count** — Throws `IllegalArgumentException` with message `"Number of  arguments must be even"`.
- **Non-string key** — Throws `ClassCastException`.
- **Mixed types** — Correctly handles `String`, `Integer`, and `Boolean` values.

## Test Architecture Summary

```
src/test/
├── java/de/tklein/tklab/openproject/mcp/
│   ├── TestConstants.java              # Shared tag constants
│   ├── tools/
│   │   ├── McpToolsHelper.java         # MCP protocol test utilities
│   │   ├── McpNoAuthTestSecurityConfig.java  # Permit-all security stub
│   │   ├── McpPingIntegrationTest.java     # ping method test
│   │   ├── McpPromptsIntegrationTest.java  # prompts/list test
│   │   ├── McpSecurityIntegrationTest.java # Auth/authorization tests
│   │   └── McpToolsListIntegrationTest.java# tools/list test
│   └── openproject/client/
│       └── OpenProjectApiClientIntegrationTest.java  # Docker + API tests
└── resources/
    ├── application-test.properties     # Container config overrides
    └── openproject/
        └── docker-compose.yml          # OpenProject container definition
```

All local MCP integration tests share a common pattern:

1. `@SpringBootTest(webEnvironment = RANDOM_PORT)` to start an embedded server.
2. `@Import(McpNoAuthTestSecurityConfig.class)` to disable auth (except `McpSecurityIntegrationTest`).
3. `@Tag(TestConstants.TAG_INTEGRATION_TEST_LOCAL)` for selective execution.
4. `@TestInstance(PER_CLASS)` for `@BeforeAll`/`@AfterAll` shared state.
5. `@LocalServerPort` to obtain the random port at runtime.
6. `McpToolsHelper` to perform raw HTTP interactions against the embedded server.

The remote `OpenProjectApiClientIntegrationTest` differs in that it uses `@Tag(TestConstants.TAG_INTEGRATION_TEST_REMOTE)`, does not use `RANDOM_PORT` (the server runs inside the container), and manages its own Docker Compose lifecycle via `ApplicationContextInitializer`.
