package de.tklein.tklab.openproject.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * common code for tests in tools package
 */
public class McpToolsHelper {

  private static final String MCP_PROTOCOL_VERSION = "2024-11-05";
  private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
  private static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
  private static final String MCP_ACCEPT = "text/event-stream, application/json";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // common client for all requests
  static final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  static String initializeAndGetSessionId(int port) throws Exception {
    return initializeAndGetSessionId(port, Map.of());
  }

  static String initializeAndGetSessionId(int port,
      Map<String, String> extraHeaders) throws Exception {

    HttpResponse<String> response = initializeMcp(port, extraHeaders);
    assertThat(response.statusCode())
        .as("expect 200 for initialize, Body: %s", response.body())
        .isEqualTo(200);

    JsonNode initJson = parseJsonRpcFromSse(response.body());
    assertThat(initJson.path("error").isMissingNode() || initJson.path("error").isNull())
        .as("don't expect initialize error, Body: %s", response.body())
        .isTrue();

    Optional<String> sessionId = response.headers().firstValue(MCP_SESSION_ID_HEADER);
    assertThat(sessionId)
        .as("missing header '%s' in initialize response. Found: %s",
            MCP_SESSION_ID_HEADER, response.headers().map())
        .isPresent();
    return sessionId.get();
  }

  static HttpResponse<String> initializeMcp(int port,
      Map<String, String> extraHeaders) throws Exception {
    URI uri = URI.create("http://localhost:" + port + "/mcp");

    String initializeRequest = """
        {
          "jsonrpc": "2.0",
          "id": "init-1",
          "method": "initialize",
          "params": {
            "protocolVersion": "%s",
            "capabilities": {},
            "clientInfo": { "name": "mcp-tools-list-test", "version": "0.0.0" }
          }
        }
        """.formatted(MCP_PROTOCOL_VERSION);

    HttpRequest.Builder rb = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(5))
        .header("Content-Type", "application/json")
        .header("Accept", MCP_ACCEPT)
        .header(MCP_PROTOCOL_VERSION_HEADER, MCP_PROTOCOL_VERSION);

    extraHeaders.forEach(rb::header);

    HttpRequest request = rb
        .POST(HttpRequest.BodyPublishers.ofString(initializeRequest))
        .build();

    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  static JsonNode callToolsList(int port, String sessionId)
      throws Exception {
    return callToolsList(port, sessionId, Map.of());
  }

  static JsonNode callToolsList(int port, String sessionId,
      Map<String, String> extraHeaders) throws Exception {

    HttpResponse<String> response = callToolsListRequest(port, sessionId, extraHeaders,
        200);
    assert response != null;
    return parseJsonRpcFromSse(response.body());
  }

  static HttpResponse<String> callToolsListRequest(int port,
      String sessionId, Map<String, String> extraHeaders, int expectStatus) throws Exception {

    URI uri = URI.create("http://localhost:" + port + "/mcp");

    String jsonRpcRequest = """
        {
          "jsonrpc": "2.0",
          "id": "1",
          "method": "tools/list",
          "params": {}
        }
        """;

    HttpRequest.Builder rb = HttpRequest.newBuilder(uri)
        .header("Content-Type", "application/json")
        .header("Accept", MCP_ACCEPT)
        .header(MCP_PROTOCOL_VERSION_HEADER, MCP_PROTOCOL_VERSION)
        .header(MCP_SESSION_ID_HEADER, sessionId);

    extraHeaders.forEach(rb::header);

    HttpRequest request = rb
        .POST(HttpRequest.BodyPublishers.ofString(jsonRpcRequest))
        .build();

    if (expectStatus == 200) {
      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode())
          .as("expect %d for tools/list, Body: %s", expectStatus, response.body())
          .isEqualTo(expectStatus);
      return response;
    } else {
      HttpResponse<Void> response = httpClient.send(request,
          HttpResponse.BodyHandlers.discarding());
      assertThat(response.statusCode())
          .as("expect %d for tools/list, Body: %s", expectStatus, response.body())
          .isEqualTo(expectStatus);
      return null;
    }
  }

  static JsonNode callPromptsList(int port, String sessionId)
      throws Exception {
    return callPromptsList(port, sessionId, Map.of());
  }

  static JsonNode callPromptsList(int port, String sessionId,
      Map<String, String> extraHeaders) throws Exception {

    HttpResponse<String> response = callPromptsListRequest(port, sessionId,
        extraHeaders,
        200);
    assert response != null;
    return parseJsonRpcFromSse(response.body());
  }

  static HttpResponse<String> callPromptsListRequest(int port,
      String sessionId, Map<String, String> extraHeaders, int expectStatus) throws Exception {

    URI uri = URI.create("http://localhost:" + port + "/mcp");

    String jsonRpcRequest = """
        {
          "jsonrpc": "2.0",
          "id": "1",
          "method": "prompts/list",
          "params": {}
        }
        """;

    HttpRequest.Builder rb = HttpRequest.newBuilder(uri)
        .header("Content-Type", "application/json")
        .header("Accept", MCP_ACCEPT)
        .header(MCP_PROTOCOL_VERSION_HEADER, MCP_PROTOCOL_VERSION)
        .header(MCP_SESSION_ID_HEADER, sessionId);

    extraHeaders.forEach(rb::header);

    HttpRequest request = rb
        .POST(HttpRequest.BodyPublishers.ofString(jsonRpcRequest))
        .build();

    if (expectStatus == 200) {
      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode())
          .as("expect %d for prompts/list, Body: %s", expectStatus, response.body())
          .isEqualTo(expectStatus);
      return response;
    } else {
      HttpResponse<Void> response = httpClient.send(request,
          HttpResponse.BodyHandlers.discarding());
      assertThat(response.statusCode())
          .as("expect %d for prompts/list, Body: %s", expectStatus, response.body())
          .isEqualTo(expectStatus);
      return null;
    }
  }

  static JsonNode callPing(int port, String sessionId)
      throws Exception {
    return callPing(port, sessionId, Map.of());
  }

  static JsonNode callPing(int port, String sessionId,
      Map<String, String> extraHeaders) throws Exception {

    HttpResponse<String> response = callPingRequest(port, sessionId, extraHeaders,
        200);
    assert response != null;
    return parseJsonRpcFromSse(response.body());
  }

  static HttpResponse<String> callPingRequest(int port,
      String sessionId, Map<String, String> extraHeaders, int expectStatus) throws Exception {

    URI uri = URI.create("http://localhost:" + port + "/mcp");

    String jsonRpcRequest = """
        {
          "jsonrpc": "2.0",
          "id": "1",
          "method": "ping",
          "params": {}
        }
        """;

    HttpRequest.Builder rb = HttpRequest.newBuilder(uri)
        .header("Content-Type", "application/json")
        .header("Accept", MCP_ACCEPT)
        .header(MCP_PROTOCOL_VERSION_HEADER, MCP_PROTOCOL_VERSION)
        .header(MCP_SESSION_ID_HEADER, sessionId);

    extraHeaders.forEach(rb::header);

    HttpRequest request = rb
        .POST(HttpRequest.BodyPublishers.ofString(jsonRpcRequest))
        .build();

    if (expectStatus == 200) {
      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode())
          .as("expect %d for ping, Body: %s", expectStatus, response.body())
          .isEqualTo(expectStatus);
      return response;
    } else {
      HttpResponse<Void> response = httpClient.send(request,
          HttpResponse.BodyHandlers.discarding());
      assertThat(response.statusCode())
          .as("expect %d for ping, Body: %s", expectStatus, response.body())
          .isEqualTo(expectStatus);
      return null;
    }
  }

  private static JsonNode parseJsonRpcFromSse(String body) {
    String trimmed = body == null ? "" : body.trim();
    // A) MCP Streaming: Body is JSON
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      return MAPPER.readTree(trimmed);
    }
    // B) MCP SSE: JSON contains data:
    String json = McpToolsHelper.extractFirstSseDataPayload(trimmed);
    assertThat(json)
        .as("No JSON payload found in body:\n%s", body)
        .isNotBlank();

    return MAPPER.readTree(json);
  }

  static String extractFirstSseDataPayload(String sseBody) {
    if (sseBody == null || sseBody.isBlank()) {
      return null;
    }
    StringBuilder data = new StringBuilder();
    boolean collecting = false;

    for (String line : sseBody.split("\\R")) { // \\R = any line break
      if (line.startsWith("data:")) {
        collecting = true;
        data.append(extractPrefixAndTrim(line, "data:"));
        continue;
      }
      if (collecting) {
        // Event endet üblicherweise mit leerer Zeile
        if (line.isBlank()) {
          break;
        }
        // Multi-line data support (selten, aber möglich)
        if (line.startsWith("data:")) {
          data.append("\n").append(extractPrefixAndTrim(line, "data:"));
        }
      }
    }

    String out = data.toString().trim();
    return out.isEmpty() ? null : out;
  }

  private static String extractPrefixAndTrim(String line, @SuppressWarnings("SameParameterValue") final @NonNull String prefix) {
    return line.substring(prefix.length()).trim();
  }

}
