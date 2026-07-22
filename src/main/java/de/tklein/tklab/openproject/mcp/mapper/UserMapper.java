package de.tklein.tklab.openproject.mcp.mapper;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import de.tklein.tklab.openproject.mcp.dto.UserDto;
import jakarta.annotation.Nonnull;
import org.mapstruct.Mapper;
import tools.jackson.databind.JsonNode;

@Mapper
public interface UserMapper {

  /**
   * Maps the OpenProject API root response (/api/v3) to a UserDto: _links.user.href -> href
   * _links.user.title -> name
   */
  default UserDto fromRoot(@Nonnull JsonNode root) {
    JsonNode userNode = root.path("_links").path("user");

    UserDto dto = new UserDto();
    dto.setHref(trimToNull(userNode.path("href").asText(null)));
    dto.setName(trimToNull(userNode.path("title").asText(null)));
    return dto;
  }
}
