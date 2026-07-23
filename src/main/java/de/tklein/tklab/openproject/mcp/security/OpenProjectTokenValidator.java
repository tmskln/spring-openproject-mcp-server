package de.tklein.tklab.openproject.mcp.security;

import de.tklein.tklab.openproject.mcp.dto.UserDto;
import de.tklein.tklab.openproject.mcp.openproject.client.OpenProjectApiClient;
import de.tklein.tklab.openproject.mcp.openproject.client.OpenProjectConnection;
import de.tklein.tklab.openproject.mcp.openproject.client.OpenProjectConnectionResolver;
import java.util.function.Supplier;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenProjectTokenValidator {

  private final OpenProjectConnectionResolver connectionResolver;
  private final OpenProjectApiClient openProjectApiClient;
  private final OpenProjectTokenValidationCache cache;

  public OpenProjectTokenValidator(
      OpenProjectConnectionResolver connectionResolver,
      OpenProjectApiClient openProjectApiClient,
      OpenProjectTokenValidationCache cache
  ) {
    this.connectionResolver = connectionResolver;
    this.openProjectApiClient = openProjectApiClient;
    this.cache = cache;
  }

  public ValidatedOpenProjectUser validateCurrentRequestOrThrow() {
    OpenProjectConnection c = connectionResolver.resolve();

    String baseUrl = (c == null || c.baseUrl() == null) ? null : c.baseUrl().toString();
    String token = (c == null) ? null : c.bearerToken();

    if (baseUrl == null || baseUrl.isBlank()) {
      throw new UnauthorizedException("Missing OpenProject baseUrl");
    }
    if (token == null || token.isBlank()) {
      throw new UnauthorizedException("Missing Bearer token");
    }

    // Hash token to be used as a cache key.
    // Token is never stored in cache!!
    String key = DigestUtils.sha256Hex(baseUrl + "|" + token);
    return cache.cachedCheckToken(key, cachedCheckToken);
  }

  /**
   * Checks the token against OpenProject. Cached!
   * Store username+href in Cache for audit and logging purposed.
   */
  final Supplier<ValidatedOpenProjectUser> cachedCheckToken = () -> {
    try {
      UserDto user = fetchUserFromOpenProject();
      if (user == null || user.getHref() == null || user.getHref().isBlank()) {
        throw new UnauthorizedException("OpenProject root() did not return a user");
      }
      return new ValidatedOpenProjectUser(user.getHref(), user.getName());
    } catch (
        RestClientResponseException e) {
      throw new UnauthorizedException("Token validation failed ("
          + e.getStatusCode() + "): " + e.getMessage());
    } catch (
        Exception e) {
      throw new UnauthorizedException("Token validation failed: " + e.getMessage());
    }
  };

  /**
   * Template method to validate current credentials against OpenProject. Overwrite in tests to
   * avoid external calls while keeping caching and keying behavior intact.
   */
  protected UserDto fetchUserFromOpenProject() {
    return openProjectApiClient.root();
  }
}