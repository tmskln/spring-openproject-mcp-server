package de.tklein.tklab.openproject.mcp.mapper;

import de.tklein.tklab.openproject.mcp.dto.TypeDto;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import tools.jackson.databind.JsonNode;

@Mapper
public interface TypeMapper {

  default TypeDto toDto(JsonNode node) {
    if (node == null) {
      return null;
    }
    TypeDto dto = new TypeDto();
    dto.setId(node.path("id").asInt());
    dto.setName(node.path("name").asString());
    dto.setDefaultType(node.path("isDefault").asBoolean());
    dto.setMilestone(node.path("isMilestone").asBoolean());

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