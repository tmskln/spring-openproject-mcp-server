package de.tklein.tklab.openproject.mcp.mapper;

import de.tklein.tklab.openproject.mcp.dto.ProjectDto;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import tools.jackson.databind.JsonNode;

@Mapper
public interface ProjectMapper {

  default ProjectDto toDto(JsonNode node) {
    if (node == null) {
      return null;
    }
    ProjectDto dto = new ProjectDto();
    dto.setId(node.path("id").asInt());
    dto.setIdentifier(node.path("identifier").asText(null));
    dto.setName(node.path("name").asText(null));
    dto.setActive(node.path("active").asBoolean());
    dto.setIsPublic(node.path("public").asBoolean());
    dto.setStatus(node.path("statusExplanation").path("raw").asText(null));
    dto.setDescription(node.path("description").path("raw").asText(null));

    String createdAt = node.path("createdAt").asText(null);
    if (createdAt != null) {
      dto.setCreatedAt(OffsetDateTime.parse(createdAt));
    }

    String updatedAt = node.path("updatedAt").asText(null);
    if (updatedAt != null) {
      dto.setUpdatedAt(OffsetDateTime.parse(updatedAt));
    }

    dto.setHref(node.path("_links").path("self").path("href").asText(null));
    return dto;
  }
}