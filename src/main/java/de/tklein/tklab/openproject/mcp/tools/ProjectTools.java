package de.tklein.tklab.openproject.mcp.tools;

import de.tklein.tklab.openproject.mcp.dto.ProjectDto;
import de.tklein.tklab.openproject.mcp.openproject.client.OpenProjectApiClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.List;
import lombok.extern.log4j.Log4j2;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
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
public class ProjectTools {

  private final OpenProjectApiClient openProjectApiClient;

  public ProjectTools(OpenProjectApiClient openProjectApiClient) {
    this.openProjectApiClient = openProjectApiClient;
  }

  @McpTool(description = "Lists existing projects visible for the current user with most relevant properties.",
      annotations = @McpAnnotations(readOnlyHint = true))
  public List<ProjectDto> projectList() {
    return openProjectApiClient.projectList();
  }

  @McpPrompt(name = "openproject.project.summary", description = "Generates a summary of project information")
  public McpSchema.GetPromptResult generateProjectSummaryPrompt(
      @McpArg(description = "Project identifier") String projectId,
      @McpArg(description = "Project name") String projectName) {

    String template = String.format("Provide a detailed summary for project %s (%s).", projectName,
        projectId);
    return GetPromptResult.builder(
            List.of(new PromptMessage(Role.ASSISTANT, TextContent.builder(template).build())))
        .description("project summary").build();
  }

}
