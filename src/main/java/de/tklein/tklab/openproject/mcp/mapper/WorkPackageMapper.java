package de.tklein.tklab.openproject.mcp.mapper;

import de.tklein.tklab.openproject.mcp.dto.WorkPackageDto;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import tools.jackson.databind.JsonNode;

@Mapper
public interface WorkPackageMapper {

  default WorkPackageDto toDto(JsonNode node) {
    if (node == null) {
      return null;
    }
    WorkPackageDto dto = new WorkPackageDto();
    dto.setId(node.path("id").asInt());
    dto.setLockVersion(node.path("lockVersion").asInt());
    dto.setSubject(node.path("subject").asString(null));
    dto.setDescription(node.path("description").path("raw").asString(null));

    JsonNode storyPointsNode = node.get("storyPoints");
    if (storyPointsNode != null && !storyPointsNode.isNull()) {
      dto.setStoryPoints(storyPointsNode.asInt());
    }

    String startDate = node.path("startDate").asString(null);
    if (startDate != null) {
      dto.setStartDate(LocalDate.parse(startDate));
    }

    String dueDate = node.path("dueDate").asString(null);
    if (dueDate != null) {
      dto.setDueDate(LocalDate.parse(dueDate));
    }

    dto.setEstimatedTime(node.path("estimatedTime").asString(null));
    dto.setDuration(node.path("duration").asString(null));

    String createdAt = node.path("createdAt").asString(null);
    if (createdAt != null) {
      dto.setCreatedAt(OffsetDateTime.parse(createdAt));
    }

    String updatedAt = node.path("updatedAt").asString(null);
    if (updatedAt != null) {
      dto.setUpdatedAt(OffsetDateTime.parse(updatedAt));
    }

    // Typ-Name extract: Für einzelne WP aus embedded, für Listen aus _links
    JsonNode typeNode = node.path("_embedded").path("type");
    if (typeNode.isMissingNode()) {
      typeNode = node.path("_links").path("type");
      dto.setType(typeNode.path("title").asString(null));
    } else {
      dto.setType(typeNode.path("name").asString(null));
    }

    // Priority-Name extrahieren: Für einzelne WP aus embedded, für Listen aus _links
    JsonNode priorityNode = node.path("_embedded").path("priority");
    if (priorityNode.isMissingNode()) {
      priorityNode = node.path("_links").path("priority");
      dto.setPriority(priorityNode.path("title").asString(null));
    } else {
      dto.setPriority(priorityNode.path("name").asString(null));
    }

    dto.setHref(node.path("_links").path("self").path("href").asString(null));
    return dto;
  }
}
