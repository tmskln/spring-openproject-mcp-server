package de.tklein.tklab.openproject.mcp.tools;

import de.tklein.tklab.openproject.mcp.dto.WorkPackageCreateDto;
import de.tklein.tklab.openproject.mcp.dto.WorkPackageCreateDto.OnCreate;
import de.tklein.tklab.openproject.mcp.dto.WorkPackageDto;
import de.tklein.tklab.openproject.mcp.dto.WorkPackageUpdateDto;
import de.tklein.tklab.openproject.mcp.openproject.client.OpenProjectApiClient;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import java.util.Base64;
import java.util.List;
import lombok.extern.log4j.Log4j2;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Copyright (c) 2026 Thomas Klein SPDX-License-Identifier: MIT
 */
@Log4j2
@Validated
@Component
public class WorkPackageTools {

  private final OpenProjectApiClient openProjectApiClient;

  public WorkPackageTools(OpenProjectApiClient openProjectApiClient) {
    this.openProjectApiClient = openProjectApiClient;
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(
      description = "Queries the work-packages by projectId.",
      annotations = @McpAnnotations(readOnlyHint = true))
  public List<WorkPackageDto> workPackageList(@NotNull Integer projectId) {
    return openProjectApiClient.workPackageList(projectId);
  }


  @SuppressWarnings("UnusedReturnValue")
  @McpTool(
      description = "Gets the work-package by id 'wpId'.",
      annotations = @McpAnnotations(readOnlyHint = true))
  public WorkPackageDto workPackageShow(@NotNull Integer workPackageId) {
    return openProjectApiClient.workPackageShow(workPackageId);
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(
      description = "Creates a new work-package for a project. Requires projectId and typeId (e.g. 1 for Task).")
  @Validated({OnCreate.class, Default.class})
  public Integer workPackageCreate(@NotNull Integer projectId,
      @Valid @NotNull WorkPackageCreateDto workPackage) {
    return openProjectApiClient.workPackageCreate(projectId, workPackage);
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(
      description = "Updates a work-package for a project. Requires projectId and typeId (e.g. 1 for Task).")
  public boolean workPackageUpdate(@NotNull Integer workPackageId,
      @Valid @NotNull WorkPackageUpdateDto workPackage) {
    return openProjectApiClient.workPackageUpdate(workPackageId, workPackage);
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(
      description = "Uploads an attachment to a work package. Provide workPackageId, fileName and base64-encoded fileContent. Optionally provide fileContentType.")
  public Integer workPackageUploadAttachment(@NotNull Integer workPackageId,
      @NotNull String fileName,
      @NotNull String fileContentBase64, @NotNull String fileContentType) {

    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(fileContentBase64);
    } catch (IllegalArgumentException _) {
      throw new IllegalArgumentException("'fileContentBase64' must be valid Base64");
    }
    return openProjectApiClient.workPackageUploadAttachment(workPackageId, fileName, bytes,
        fileContentType);
  }

  @McpPrompt(
      name = "openproject.workpackage.safe_edit",
      description = "Safe edit workflow for OpenProject Work Packages (diff + confirm, lockVersion handling, Markdown newline directive).")
  public GetPromptResult safeEditWorkPackagePrompt(
      @McpArg(name = "workPackageId", description = "ID of the OpenProject Work Package to modify.", required = true)
      String workPackageId,
      @McpArg(name = "requestedChange", description = "Natural language description of what should be changed.", required = true)
      String requestedChange
  ) {
    String prompt = """
        You are an MCP assistant connected to OpenProject. Your job is to propose and apply safe updates to Work Packages.
        
        Target Work Package:
        - workPackageId: %s
        
        User request:
        - requestedChange: %s
        
        ## Critical formatting rule (Markdown newlines)
        - When you output or construct Markdown for Work Package text/description fields, represent line breaks using the literal two-character sequence /n.
        - Do NOT use //n.
        - Only deviate from this rule if the user explicitly revokes this directive (they must clearly say so). If revoked, follow the user's new instruction exactly.
        
        ## Concurrency & locking (lockVersion)
        - For any work_package_update, you MUST include the property lockVersion from the latest known server state of that Work Package.
        - After performing an update, treat your local Work Package state as stale.
          - If further changes are needed, reload/refetch the Work Package first and use the newest lockVersion.
        
        ## Safe-change workflow (Diff + explicit confirmation)
        1. Understand the requested change.
        2. Fetch the current Work Package (or ensure you have the latest state).
        3. Prepare a patch proposal and show it as a unified diff (old vs new), including all affected fields.
           - If editing Markdown text, ensure newlines are expressed as /n in the proposed new text.
        4. Ask for explicit confirmation before calling work_package_update.
           - Accept only an unambiguous confirmation like: "Yes, apply", "Confirm", or "Proceed".
           - If the user requests modifications, revise the diff and ask again.
        5. On confirmation:
           - Execute work_package_update using the newest lockVersion.
        6. After update:
           - If you need to do anything else (additional edits, verification, follow-up changes), reload/refetch the Work Package first.
        
        ## Output requirements
        - Always present the diff BEFORE any update call.
        - Never apply updates without confirmation.
        - If there is a lockVersion conflict, reload/refetch, regenerate the diff, and re-request confirmation.
        """.formatted(workPackageId, requestedChange);

    return GetPromptResult.builder(
            List.of(new PromptMessage(Role.ASSISTANT, TextContent.builder(prompt).build())))
        .description("OpenProject Work Package Safe Edit").build();
  }
}