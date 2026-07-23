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
    dto.setIdentifier(node.path("identifier").asString(null));
    dto.setName(node.path("name").asString(null));
    dto.setActive(node.path("active").asBoolean());
    dto.setIsPublic(node.path("public").asBoolean());
    dto.setStatus(node.path("statusExplanation").path("raw").asString(null));
    dto.setDescription(node.path("description").path("raw").asString(null));

    String createdAt = node.path("createdAt").asString(null);
    if (createdAt != null) {
      dto.setCreatedAt(OffsetDateTime.parse(createdAt));
    }

    String updatedAt = node.path("updatedAt").asString(null);
    if (updatedAt != null) {
      dto.setUpdatedAt(OffsetDateTime.parse(updatedAt));
    }

    dto.setHref(node.path("_links").path("self").path("href").asString(null));
    return dto;
  }
}