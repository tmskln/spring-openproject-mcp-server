package de.tklein.tklab.openproject.mcp.openproject.client;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HttpHeaderOpenProjectConnectionResolver implements OpenProjectConnectionResolver {

  public static final String HEADER_BASE_URL = "X-OpenProject-Base-Url";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String BEARER = "Bearer ";

  private final URI configuredBaseUrl;
  private final boolean allowHeaderBaseUrl;

  public HttpHeaderOpenProjectConnectionResolver(
      @Value("${openproject.url}") String configuredBaseUrl,
      @Value("${openproject.allow-header-base-url:false}") boolean allowHeaderBaseUrl
  ) {
    this.configuredBaseUrl = URI.create(configuredBaseUrl);
    this.allowHeaderBaseUrl = allowHeaderBaseUrl;
  }

  @Override
  public OpenProjectConnection resolve() {
    HttpServletRequest req = currentRequest();

    String authorization = (req == null) ? null : req.getHeader(HEADER_AUTHORIZATION);
    String bearerToken = extractBearerToken(authorization).orElse(null);

    URI baseUrl = configuredBaseUrl;
    if (allowHeaderBaseUrl && req != null) {
      String headerUrl = req.getHeader(HEADER_BASE_URL);
      if (headerUrl != null && !headerUrl.isBlank()) {
        baseUrl = URI.create(headerUrl.trim());
      }
    }

    return new OpenProjectConnection(baseUrl, bearerToken);
  }

  private static HttpServletRequest currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
      return servletRequestAttributes.getRequest();
    }
    return null;
  }

  private static Optional<String> extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null) {
      return Optional.empty();
    }
    String s = authorizationHeader.trim();
    if (s.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
      String token = s.substring(BEARER.length()).trim();
      return token.isBlank() ? Optional.empty() : Optional.of(token);
    }
    return Optional.empty();
  }
}