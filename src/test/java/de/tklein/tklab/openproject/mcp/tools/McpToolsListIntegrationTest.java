package de.tklein.tklab.openproject.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import de.tklein.tklab.openproject.mcp.TestConstants;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Tests behavior around MCP method: 'tools/list'
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
class McpToolsListIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @LocalServerPort
  int port;

  private JsonNode toolsListResponse;

  @BeforeAll
  void initOnce() throws Exception {
    String sessionId = McpToolsHelper.initializeAndGetSessionId(port);
    this.toolsListResponse = McpToolsHelper.callToolsList(port, sessionId);
  }

  @Test
  void toolsList_jsonRpcBasics() {
    // JSON-RPC Basis-Checks
    assertThat(toolsListResponse.path("jsonrpc").asText()).isEqualTo("2.0");
    assertThat(toolsListResponse.hasNonNull("id")).isTrue();
    assertThat(toolsListResponse.path("error").isMissingNode() || toolsListResponse.path("error")
        .isNull()).isTrue();
  }

  @Test
  void toolsList_shouldReturnNonEmptyToolsArray() {
    JsonNode tools = toolsListResponse.path("result").path("tools");
    assertThat(tools.isArray()).isTrue();

    List<String> expectedToolNames = List.of(
        "priorityList",
        "projectList",
        "relationAdd",
        "relationAddParent",
        "relationAllowedValues",
        "relationDelete",
        "relationDeleteParent",
        "relationList",
        "typeList",
        "workPackageCreate",
        "workPackageList",
        "workPackageShow",
        "workPackageUpdate",
        "workPackageUploadAttachment"
    );
    List<String> actualToolNames = tools.findValuesAsString("name");
    assertThat(actualToolNames).containsExactlyInAnyOrderElementsOf(expectedToolNames);
  }

  @Test
  void toolsList_allToolsHaveNameDescriptionAndInputSchema() {
    JsonNode tools = toolsListResponse.path("result").path("tools");
    assertThat(tools.isArray()).isTrue();
    assertThat(tools).isNotEmpty();
    // tools.forEach(c -> System.out.println(c.get("name").asText()));

    // Generische Qualitäts-Checks für alle Tools
    for (JsonNode tool : tools) {
      assertThat(tool.path("name").asText()).isNotBlank();
      assertThat(tool.path("description").asText()).isNotBlank();
      assertThat(tool.path("inputSchema").isObject()).isTrue();
      assertThat(tool.path("inputSchema").path("type").asText()).isIn("object", "");
    }
  }

  @Test
  void toolsList_relationAdd_hasRequiredParametersAndPropertyDescriptions() {
    JsonNode tools = toolsListResponse.path("result").path("tools");
    assertThat(tools.isArray()).isTrue();
    assertThat(tools).isNotEmpty();

    // Konkrete Checks für ein Tool mit required-Parametern + Feld-Descriptions
    JsonNode relationAdd = findToolByName(tools, "relationAdd");
    assertThat(relationAdd).isNotNull();
    assertThat(relationAdd.path("description").asText())
        .isEqualTo("Adds a relation to a work package.");

    JsonNode inputSchema = relationAdd.path("inputSchema");
    JsonNode required = inputSchema.path("required");
    assertThat(required.isArray()).isTrue();

    assertThat(requiredAsSet(required))
        .contains("workPackageId", "otherWorkPackageId", "relationType");

    JsonNode properties = inputSchema.path("properties");
    assertThat(properties.path("workPackageId").path("description").asText())
        .contains("work package unique id (from)");
    assertThat(properties.path("otherWorkPackageId").path("description").asText())
        .contains("work package unique id (to)");
    assertThat(properties.path("description").path("description").asText())
        .contains("optional description of the relation");
    assertThat(properties.path("relationType").path("description").asText())
        .contains("allowed relation type");
  }

  @Test
  void toolsList_canBeAssertedWithJsonPath_toolNamesContainRelationAdd_andNoBlankNames() {
    String json = objectMapper.writeValueAsString(toolsListResponse);
    DocumentContext ctx = JsonPath.parse(json);

    List<String> names = ctx.read("$.result.tools[*].name");
    assertThat(names).isNotNull()
        .isNotEmpty()
        .contains("relationAdd")
        .allSatisfy(n -> assertThat(n).isNotBlank());
  }

  private JsonNode findToolByName(JsonNode toolsArray, String name) {
    for (JsonNode tool : toolsArray) {
      if (name.equals(tool.path("name").asText())) {
        return tool;
      }
    }
    return null;
  }

  private Set<String> requiredAsSet(JsonNode requiredArray) {
    Set<String> out = new HashSet<>();
    for (JsonNode n : requiredArray) {
      out.add(n.asText());
    }
    return out;
  }
}