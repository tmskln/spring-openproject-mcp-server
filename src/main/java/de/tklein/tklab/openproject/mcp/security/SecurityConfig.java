package de.tklein.tklab.openproject.mcp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  InMemoryUserDetailsManager actuatorUsers(
      PasswordEncoder encoder,
      @Value("${actuator.security.admin.username:admin}") String username,
      @Value("${actuator.security.admin.password-hash}") String passwordHash
  ) {
    // passwordHash MUST include an id prefix like {bcrypt}...
    UserDetails admin = User.withUsername(username)
        .password(passwordHash)
        .roles("ACTUATOR_ADMIN")
        .build();
    return new InMemoryUserDetailsManager(admin);
  }

  /**
   * IMPORTANT: McpBearerAuthenticationFilter is a Filter bean (@Component) and would otherwise be
   * registered as a global servlet filter by Spring Boot. We want it ONLY inside the Spring
   * Security chain.
   */
  @Bean
  FilterRegistrationBean<McpBearerAuthenticationFilter> disableGlobalMcpFilterRegistration(
      McpBearerAuthenticationFilter filter
  ) {
    FilterRegistrationBean<McpBearerAuthenticationFilter> frb = new FilterRegistrationBean<>(
        filter);
    frb.setEnabled(false);
    return frb;
  }

  @Bean
  @Order(1)
  SecurityFilterChain actuatorSecurity(HttpSecurity http) {
    http
        .securityMatcher("/actuator/**")
        .authorizeHttpRequests(reg -> reg
            .requestMatchers("/actuator/health", "/actuator/health/*").permitAll()
            .anyRequest().hasRole("ACTUATOR_ADMIN"))
        .httpBasic(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain mcpSecurity(HttpSecurity http, McpBearerAuthenticationFilter mcpFilter) {
    http
        .securityMatcher("/mcp/**", "/mcp", "/error", "/sse")
        .authorizeHttpRequests(reg -> reg.anyRequest().authenticated())
        .addFilterBefore(mcpFilter, UsernamePasswordAuthenticationFilter.class)
        .csrf(AbstractHttpConfigurer::disable);

    // No httpBasic here; MCP auth is bearer-based
    http.httpBasic(AbstractHttpConfigurer::disable);
    return http.build();
  }
}
