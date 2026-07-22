package de.tklein.tklab.openproject.mcp.mapper;

import tools.jackson.databind.JsonNode;
import de.tklein.tklab.openproject.mcp.dto.TypeDto;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;

@Mapper
public interface TypeMapper {

    default TypeDto toDto(JsonNode node) {
        if (node == null) {
            return null;
        }
        TypeDto dto = new TypeDto();
        dto.setId(node.path("id").asInt());
        dto.setName(node.path("name").asText());
        dto.setDefaultType(node.path("isDefault").asBoolean());
        dto.setMilestone(node.path("isMilestone").asBoolean());

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