package de.tklein.tklab.openproject.mcp.openproject.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HttpHeaderOpenProjectConnectionResolverTest {

  @AfterEach
  void clearRequestContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void resolveUsesCurrentRequestAuthorizationHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaderOpenProjectConnectionResolver.HEADER_AUTHORIZATION,
        "Bearer test-token");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    var resolver = new HttpHeaderOpenProjectConnectionResolver("https://configured.example", false);

    OpenProjectConnection connection = resolver.resolve();

    assertThat(connection.baseUrl()).hasToString("https://configured.example");
    assertThat(connection.bearerToken()).isEqualTo("test-token");
  }

  @Test
  void resolveUsesHeaderBaseUrlWhenEnabled() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaderOpenProjectConnectionResolver.HEADER_AUTHORIZATION,
        "Bearer test-token");
    request.addHeader(HttpHeaderOpenProjectConnectionResolver.HEADER_BASE_URL,
        "https://header.example");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    var resolver = new HttpHeaderOpenProjectConnectionResolver("https://configured.example", true);

    OpenProjectConnection connection = resolver.resolve();

    assertThat(connection.baseUrl()).hasToString("https://header.example");
    assertThat(connection.bearerToken()).isEqualTo("test-token");
  }

  @Test
  void resolveFallsBackWhenNoRequestIsBound() {
    var resolver = new HttpHeaderOpenProjectConnectionResolver("https://configured.example", true);

    OpenProjectConnection connection = resolver.resolve();

    assertThat(connection.baseUrl()).hasToString("https://configured.example");
    assertThat(connection.bearerToken()).isNull();
  }
}
