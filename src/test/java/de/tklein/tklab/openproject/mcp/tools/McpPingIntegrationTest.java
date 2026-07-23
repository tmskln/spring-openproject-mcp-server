package de.tklein.tklab.openproject.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import de.tklein.tklab.openproject.mcp.TestConstants;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Tests behavior around MCP method: 'ping'.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        // Testing tools/list with STREAMABLE here even though app uses SSE
        // this avoids open stream and the result is expected to be the same
        "spring.ai.mcp.server.protocol=STREAMABLE"
    }
)
@Import(McpNoAuthTestSecurityConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag(TestConstants.TAG_INTEGRATION_TEST_LOCAL)
class McpPingIntegrationTest {

  @LocalServerPort
  int port;

  @Test
  void ping_jsonRpcBasics() throws Exception {
    String sessionId = McpToolsHelper.initializeAndGetSessionId(port);
    JsonNode pingResponse = McpToolsHelper.callPing(port, sessionId);
    // JSON-RPC Basic checks
    assertThat(pingResponse.path("jsonrpc").asString()).isEqualTo("2.0");
    assertThat(pingResponse.hasNonNull("id")).isTrue();
    assertThat(pingResponse.path("error").isMissingNode()
        || pingResponse.path("error").isNull()).isTrue();
    JsonNode result = pingResponse.path("result");
    assertThat(result.isObject()).isTrue(); // assuming asn empty result for ping
  }

}
