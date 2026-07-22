package de.tklein.tklab.openproject.mcp.mapper;

import de.tklein.tklab.openproject.mcp.dto.RelationDto;
import de.tklein.tklab.openproject.mcp.openproject.client.OpenProjectApiClient;
import org.mapstruct.Mapper;
import tools.jackson.databind.JsonNode;

@Mapper
public interface RelationMapper {

  default RelationDto fromRelationElement(JsonNode relationElement) {
    if (relationElement == null || relationElement.isNull()) {
      return null;
    }

    RelationDto dto = new RelationDto();

    JsonNode idNode = relationElement.path("id");
    dto.setId(idNode.isMissingNode() || idNode.isNull() ? null : idNode.asInt());

    dto.setType(relationElement.path("type").asText(null));
    dto.setDescription(relationElement.path("description").asText(null));

    JsonNode to = relationElement.path("_links").path("to");
    dto.setToId(extractWpId(to.path("href").asText(null)));
    dto.setTo(to.path("title").asText(null));

    return dto;
  }

  default RelationDto fromWpLink(String derivedType, JsonNode linkNode) {
    if (linkNode == null || linkNode.isNull() || linkNode.isMissingNode()) {
      return null;
    }

    RelationDto dto = new RelationDto();
    dto.setId(null); // derived entries: parent/children/ancestors
    dto.setType(derivedType);
    dto.setToId(extractWpId(linkNode.path("href").asText(null)));
    dto.setTo(linkNode.path("title").asText(null));

    return dto;
  }

  default Integer extractWpId(String href) {
    if (href == null) {
      return null;
    }
    String part = href.replace(OpenProjectApiClient.WP_PREFIX, "");
    Integer id = null;
    try {
      id = Integer.parseInt(part);
    } catch (NumberFormatException _) {
      // TODO log.warn("href='{}' hasn't expected prefix '{}'",href,OpenProjectApiClient.WP_PREFIX)
    }
    return id;
  }

}
