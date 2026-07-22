package de.tklein.tklab.openproject.mcp.mapper;

import tools.jackson.databind.JsonNode;
import de.tklein.tklab.openproject.mcp.dto.WorkPackageDto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;

@Mapper
public interface WorkPackageMapper {

    default WorkPackageDto toDto(JsonNode node) {
        if (node == null) {
            return null;
        }
        WorkPackageDto dto = new WorkPackageDto();
        dto.setId(node.path("id").asInt());
        dto.setLockVersion(node.path("lockVersion").asInt());
        dto.setSubject(node.path("subject").asText(null));
        dto.setDescription(node.path("description").path("raw").asText(null));

        JsonNode storyPointsNode = node.get("storyPoints");
        if (storyPointsNode != null && !storyPointsNode.isNull()) {
            dto.setStoryPoints(storyPointsNode.asInt());
        }

        String startDate = node.path("startDate").asText(null);
        if (startDate != null) {
            dto.setStartDate(LocalDate.parse(startDate));
        }

        String dueDate = node.path("dueDate").asText(null);
        if (dueDate != null) {
            dto.setDueDate(LocalDate.parse(dueDate));
        }

        dto.setEstimatedTime(node.path("estimatedTime").asText(null));
        dto.setDuration(node.path("duration").asText(null));

        String createdAt = node.path("createdAt").asText(null);
        if (createdAt != null) {
            dto.setCreatedAt(OffsetDateTime.parse(createdAt));
        }

        String updatedAt = node.path("updatedAt").asText(null);
        if (updatedAt != null) {
            dto.setUpdatedAt(OffsetDateTime.parse(updatedAt));
        }

        // Typ-Name extract: Für einzelne WP aus embedded, für Listen aus _links
        JsonNode typeNode = node.path("_embedded").path("type");
        if (typeNode.isMissingNode()) {
            typeNode = node.path("_links").path("type");
            dto.setType(typeNode.path("title").asText(null));
        } else {
            dto.setType(typeNode.path("name").asText(null));
        }

        // Priority-Name extrahieren: Für einzelne WP aus embedded, für Listen aus _links
        JsonNode priorityNode = node.path("_embedded").path("priority");
        if (priorityNode.isMissingNode()) {
            priorityNode = node.path("_links").path("priority");
            dto.setPriority(priorityNode.path("title").asText(null));
        } else {
            dto.setPriority(priorityNode.path("name").asText(null));
        }

        dto.setHref(node.path("_links").path("self").path("href").asText(null));
        return dto;
    }
}
