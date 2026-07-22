package de.tklein.tklab.openproject.mcp.tools;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Override auth in mcpSecurity(...)
 * {@link de.tklein.tklab.openproject.mcp.security.SecurityConfig}
 */
@TestConfiguration
public class McpNoAuthTestSecurityConfig {

  @Bean
  @Order(0)
  SecurityFilterChain testMcpPermitAll(HttpSecurity http) {
    http
        .securityMatcher("/mcp", "/mcp/**", "/sse")
        .authorizeHttpRequests(reg -> reg.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);
    return http.build();
  }
}
