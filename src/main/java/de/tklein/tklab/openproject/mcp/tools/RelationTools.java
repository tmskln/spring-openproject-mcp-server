package de.tklein.tklab.openproject.mcp.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import de.tklein.tklab.openproject.mcp.dto.RelationDto;
import de.tklein.tklab.openproject.mcp.dto.RelationValuesDto;
import de.tklein.tklab.openproject.mcp.openproject.client.OpenProjectApiClient;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collection;
import lombok.extern.log4j.Log4j2;
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
public class RelationTools {

  public enum RelationType {
    relates,
    blocks,
    precedes,
    duplicates,
    includes,
    requires
  }

  private final OpenProjectApiClient openProjectApiClient;

  public RelationTools(OpenProjectApiClient openProjectApiClient) {
    this.openProjectApiClient = openProjectApiClient;
  }


  @McpTool(
      description = "Allowed values for relations as 'relationType'",
      annotations = @McpAnnotations(readOnlyHint = true))
  public RelationValuesDto relationAllowedValues() {
    return new RelationValuesDto(
        Arrays.stream(RelationType.values()).map(Enum::name).toArray(String[]::new));
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(
      description = "List relations of the work package references by unique work package id.",
      annotations = @McpAnnotations(readOnlyHint = true))
  public Collection<RelationDto> relationList(
      @NotNull Integer workPackageId) {
    return openProjectApiClient.listRelationsForWorkPackage(workPackageId);
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(
      description = "Adds a relation to a work package.")
  public boolean relationAdd(
      @JsonPropertyDescription("work package unique id (from)") @NotNull Integer workPackageId,
      @JsonPropertyDescription("work package unique id (to)") @NotNull Integer otherWorkPackageId,
      @JsonPropertyDescription("optional description of the relation") @JsonProperty String description,
      @JsonPropertyDescription("allowed relation type from MCP tool 'relationAllowedValues'") @NotNull RelationType relationType) {
    return openProjectApiClient.relationAdd(workPackageId, otherWorkPackageId, description,
        relationType);
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(description = "Deletes a work package's relation references by relation's unique id.")
  public boolean relationDelete(
      @JsonPropertyDescription("id from MCP tool 'relationList'") @NotNull Integer relationId) {
    return openProjectApiClient.relationDelete(relationId);
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(description = "Adds the 'parent' reference to a work package identified by work package unique id.")
  public boolean relationAddParent(
      @NotNull Integer workPackageId,
      @NotNull Integer parentWorkPackageIdId) {
    return openProjectApiClient.relationAddParent(workPackageId, parentWorkPackageIdId);
  }

  @SuppressWarnings("UnusedReturnValue")
  @McpTool(description = "Deletes the 'parent' reference of a work package identified by work package unique id.")
  public boolean relationDeleteParent(@NotNull Integer workPackageId) {
    return openProjectApiClient.relationDeleteParent(workPackageId);
  }

}
