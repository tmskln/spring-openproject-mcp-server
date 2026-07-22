package de.tklein.tklab.openproject.mcp.tools;

import de.tklein.tklab.openproject.mcp.dto.TypeDto;
import de.tklein.tklab.openproject.mcp.openproject.client.OpenProjectApiClient;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Copyright (c) 2026 Thomas Klein
 * SPDX-License-Identifier: MIT
 */
@Log4j2
@Validated
@Component
public class TypesTools {

  private final OpenProjectApiClient openProjectApiClient;

  public TypesTools(OpenProjectApiClient openProjectApiClient) {
    this.openProjectApiClient = openProjectApiClient;
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(description = "List the allowed types a work package can have in a certain project.",
      annotations = @McpAnnotations(readOnlyHint = true))
  public List<TypeDto> typeList(@NotNull Integer projectId) {
    return openProjectApiClient.typeList(projectId);
  }
}
