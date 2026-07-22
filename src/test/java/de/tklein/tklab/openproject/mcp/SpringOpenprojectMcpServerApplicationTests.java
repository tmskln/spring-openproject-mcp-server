package de.tklein.tklab.openproject.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the Spring Boot application context starts with the expected test profile and
 * keeps critical autoconfigured infrastructure in place after dependency upgrades.
 */
@SpringBootTest
@ActiveProfiles("test")
class SpringOpenprojectMcpServerApplicationTests {

  @Autowired
  private CacheManager cacheManager;

  @Test
  void contextLoads() {
    // empty test to verify that the Spring Boot application context starts successfully
  }


  /**
    Guards the Spring Boot 4 migration fix: the cache starter plus the Caffeine dependency must
    continue to produce a {@link CaffeineCacheManager} instead of silently switching to another
    cache implementation when dependencies change.
   */
  @Test
  void usesCaffeineCacheManager() {
    assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
    assertThat(cacheManager.getCache("auth-validation")).isNotNull();
  }

}
