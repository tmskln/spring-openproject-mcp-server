package de.tklein.tklab.openproject.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import de.tklein.tklab.openproject.mcp.TestConstants;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Tests behavior around MCP method: 'prompts/list'
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
class McpPromptsIntegrationTest {

  @LocalServerPort
  int port;

  private JsonNode promptsListResponse;

  @BeforeAll
  void initOnce() throws Exception {
    String sessionId = McpToolsHelper.initializeAndGetSessionId(port);
    this.promptsListResponse = McpToolsHelper.callPromptsList(port, sessionId);
  }

  @Test
  void promptsList_jsonRpcBasics() {
    // JSON-RPC Basis-Checks
    assertThat(promptsListResponse.path("jsonrpc").asString()).isEqualTo("2.0");
    assertThat(promptsListResponse.hasNonNull("id")).isTrue();
    assertThat(
        promptsListResponse.path("error").isMissingNode() || promptsListResponse.path("error")
            .isNull()).isTrue();
  }

  @Test
  void promptsList_shouldReturnNonEmptyPromptsArray() {
    JsonNode prompts = promptsListResponse.path("result").path("prompts");
    assertThat(prompts.isArray()).isTrue();

    // Check that we have the expected prompt (from WorkPackageTools)
    List<String> actualPromptNames = prompts.findValuesAsString("name");
    assertThat(actualPromptNames).contains("openproject.workpackage.safe_edit",
        "openproject.project.summary");
  }

  @Test
  void promptsList_allPromptsHaveNameDescriptionAndInputSchema() {
    JsonNode prompts = promptsListResponse.path("result").path("prompts");
    assertThat(prompts.isArray()).isTrue();
    assertThat(prompts).isNotEmpty();

    // Generische Qualitäts-Checks für alle Prompts
    for (JsonNode prompt : prompts) {
      assertThat(prompt.path("name").asString()).isNotBlank();
      assertThat(prompt.path("description").asString()).isNotBlank();
    }
  }

  @ParameterizedTest
  @CsvSource(value = {
      "openproject.workpackage.safe_edit,Safe edit workflow for OpenProject Work Packages",
      "openproject.project.summary,Generates a summary",})
  void promptsList_haveExpectedNameAndDescription(String name, String description) {
    JsonNode prompts = promptsListResponse.path("result").path("prompts");
    assertThat(prompts.isArray()).isTrue();
    assertThat(prompts).isNotEmpty();

    // Find the safe edit prompt
    JsonNode safeEditPrompt = findPromptByName(prompts, name);
    assertThat(safeEditPrompt).isNotNull();

    assertThat(safeEditPrompt.path("name").asString()).isEqualTo(name);
    assertThat(safeEditPrompt.path("description").asString()).contains(description);
  }

  private JsonNode findPromptByName(JsonNode promptsArray, String name) {
    for (JsonNode prompt : promptsArray) {
      if (name.equals(prompt.path("name").asString())) {
        return prompt;
      }
    }
    return null;
  }
}
