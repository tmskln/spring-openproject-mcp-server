package de.tklein.tklab.openproject.mcp.mapper;

import tools.jackson.databind.JsonNode;
import de.tklein.tklab.openproject.mcp.dto.PriorityDto;
import org.mapstruct.Mapper;

@Mapper
public interface PriorityMapper {

  default PriorityDto toDto(JsonNode node) {
    if (node == null) {
      return null;
    }

    PriorityDto dto = new PriorityDto();
    dto.setId(node.path("id").asInt());
    dto.setName(node.path("name").asText(null));
    dto.setPosition(node.path("position").isMissingNode() ? null : node.path("position").asInt());
    dto.setHref(node.path("_links").path("self").path("href").asText(null));
    return dto;
  }
}
