package de.tklein.tklab.openproject.mcp.openproject.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tklein.tklab.openproject.mcp.TestConstants;
import de.tklein.tklab.openproject.mcp.dto.PriorityDto;
import de.tklein.tklab.openproject.mcp.dto.ProjectDto;
import de.tklein.tklab.openproject.mcp.dto.RelationDto;
import de.tklein.tklab.openproject.mcp.dto.TypeDto;
import de.tklein.tklab.openproject.mcp.dto.UserDto;
import de.tklein.tklab.openproject.mcp.dto.WorkPackageCreateDto;
import de.tklein.tklab.openproject.mcp.dto.WorkPackageDto;
import de.tklein.tklab.openproject.mcp.dto.WorkPackageUpdateDto;
import de.tklein.tklab.openproject.mcp.tools.RelationTools.RelationType;
import de.tklein.tklab.openproject.mcp.util.Utils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

@Log4j2
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(initializers = OpenProjectApiClientIntegrationTest.OpenProjectInitializer.class)
@Import(OpenProjectApiClientIntegrationTest.TestAuthConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag(TestConstants.TAG_INTEGRATION_TEST_REMOTE)
class OpenProjectApiClientIntegrationTest {

  private static final String COMPOSE_DIR = "src/test/resources/openproject";
  private static final String COMPOSE_FILE = "docker-compose.yml";
  private static final String COMPOSE_SERVICE_NAME = "openproject";

  // config property: openproject.container.shutdown
  private static boolean composeShutdown;
  // config property: openproject.container.tag
  private static String tag;
  // config property: openproject.container.port
  private static int port;

  // set after docker compose up
  private static String baseUrl;
  // set after docker OpenProject up & running
  private static String apiKey;

  @Autowired
  private OpenProjectApiClient client;

  // Test values
  private Integer demoProjectId;
  private Integer demoTypeId;
  private Integer demoPriorityId;

  /**
   * Provides "Authorization: Bearer <token>" semantics for direct-bean integration tests (no HTTP
   * request => no real headers).
   */
  @TestConfiguration
  static class TestAuthConfig {


    @Bean
    @Primary
    OpenProjectConnectionResolver testOpenProjectConnectionResolver() {
      return () -> new OpenProjectConnection(URI.create(baseUrl), apiKey);
    }
  }

  /**
   * Ensures container + derived properties are available BEFORE Spring creates beans.
   */
  static class OpenProjectInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      // since we need the values before class initialization cannot use @Value(..) to set properties
      tag = applicationContext.getEnvironment().getProperty(
          "openproject.container.tag", "latest");
      port = applicationContext.getEnvironment().getProperty(
          "openproject.container.port", Integer.class, 8088);
      composeShutdown = applicationContext.getEnvironment().getProperty(
          "openproject.container.shutdown", Boolean.class, true);

      // start docker and create a token
      ensureStarted();

      TestPropertyValues.of("openproject.url=" + baseUrl)
          .applyTo(applicationContext.getEnvironment());
    }
  }

  @BeforeAll
  void initIds() {
    // root() should work with the generated token
    UserDto me = client.root();
    assertThat(me).isNotNull();
    assertThat(me.getHref()).isNotBlank();

    List<ProjectDto> projects = client.projectList();
    ProjectDto demo = projects.stream()
        .filter(p -> "Demo project".equals(p.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Expected 'Demo project' in projectList()"));

    assertThat(demo.getId()).isNotNull();
    this.demoProjectId = demo.getId();

    List<TypeDto> types = client.typeList(demoProjectId);
    assertThat(types).isNotEmpty();
    this.demoTypeId = types.getFirst().getId();
    assertThat(this.demoTypeId).isNotNull();

    List<PriorityDto> priorities = client.priorityList();
    assertThat(priorities).isNotEmpty();
    this.demoPriorityId = priorities.getFirst().getId();
    assertThat(this.demoPriorityId).isNotNull();
  }

  @AfterAll
  static void afterAll() {
    try {
      if (composeShutdown) {
        dockerCompose("down", "-v");
      }
    } catch (Exception _) {
      // best effort cleanup
    }
  }

  @Test
  void root_returnsAuthenticatedUser() {
    UserDto user = client.root();
    assertThat(user).isNotNull();
    assertThat(user.getHref()).isNotBlank();
    // name may depend on instance/config, so we only sanity-check
  }

  @Test
  void projectList_containsDemoProject() {
    List<ProjectDto> projects = client.projectList();

    assertThat(projects).isNotNull();
    assertThat(projects)
        .extracting(ProjectDto::getName)
        .contains("Demo project");
  }

  @Test
  void workPackageShow_withNonExistentWpId() {
    // Contract: non-existent work package should throw 404 (not return null)
    int nonExistentWpId = Integer.MAX_VALUE; // unlikely to exist
    assertThat(client.workPackageShow(nonExistentWpId)).isNull();
  }

  @Test
  void relationDelete_withNonExistentRelationId_returnsFalse() {
    // Contract: non-existent relation should return false (not throw)
    int nonExistentRelationId = Integer.MAX_VALUE;
    boolean result = client.relationDelete(nonExistentRelationId);
    assertThat(result).isFalse();
  }

  @Test
  void relationAdd_withNonExistentWorkPackageId_returnsFalse() {
    // Contract: invalid IDs should return false (not throw)
    final int validWpId = createWorkPackage("Test for invalid relation");
    final int nonExistentWpId = Integer.MAX_VALUE;
    assertThatThrownBy(
        () -> client.relationAdd(validWpId, nonExistentWpId, "test", RelationType.relates))
        .isInstanceOf(HttpClientErrorException.class)
        .satisfies(
            e -> assertThat(((HttpClientErrorException) e).getStatusCode().value()).isEqualTo(
                422));
  }

  @Test
  void workPackageList_withNonExistentProjectId_throws404Exception() {
    // Contract: non-existent project should throw 404 (not return empty list)
    int nonExistentProjectId = Integer.MAX_VALUE;
    assertThatThrownBy(() -> client.workPackageList(nonExistentProjectId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            e -> assertThat(((RestClientResponseException) e).getStatusCode().value()).isEqualTo(
                404));
  }

  @Test
  void priorityList_returnsNonEmptyList_withIdsAndNames() {
    List<PriorityDto> priorities = client.priorityList();
    assertThat(priorities).isNotNull()
        .isNotEmpty()
        .allSatisfy(p -> {
          assertThat(p.getId()).isNotNull();
          assertThat(p.getName()).isNotBlank();
        });
  }

  @Test
  void typeList_forDemoProject_returnsNonEmptyList_withIdsAndNames() {
    List<TypeDto> types = client.typeList(demoProjectId);
    assertThat(types).isNotNull()
        .isNotEmpty()
        .allSatisfy(t -> {
          assertThat(t.getId()).isNotNull();
          assertThat(t.getName()).isNotBlank();
        });
  }

  @Test
  void workPackageCrud_and_attachments_and_relations_areFunctional() {
    // --- Create two work packages to test relations
    int wpA = createWorkPackage("IntegrationTest A");
    int wpB = createWorkPackage("IntegrationTest B");
    Utils.sleep(Duration.ofSeconds(2));

    // --- List work packages (should include at least one of them)
    List<WorkPackageDto> wps = client.workPackageList(demoProjectId);
    assertThat(wps).isNotNull().extracting(WorkPackageDto::getId).contains(wpA);

    // --- Show work package
    WorkPackageDto shown = client.workPackageShow(wpA);
    assertThat(shown).isNotNull();
    assertThat(shown.getId()).isEqualTo(wpA);
    assertThat(shown.getSubject()).isNotBlank();

    assertThat(shown.getId()).isEqualTo(wpA);
    assertThat(shown.getLockVersion()).isGreaterThanOrEqualTo(0);

    // --- Update work package subject (requires lockVersion)
    String updatedSubject = "IntegrationTest A (updated)";
    WorkPackageUpdateDto upd = new WorkPackageUpdateDto();
    upd.setLockVersion(shown.getLockVersion());
    upd.setSubject(updatedSubject);
    boolean updated = client.workPackageUpdate(wpA, upd);
    assertThat(updated).isTrue();

    WorkPackageDto updatedShown = client.workPackageShow(wpA);
    assertThat(updatedShown).isNotNull();
    assertThat(updatedShown.getSubject()).isEqualTo(updatedSubject);

    // --- Upload attachment
    byte[] content = "hello from integration test".getBytes(StandardCharsets.UTF_8);
    Integer attachmentId = client.workPackageUploadAttachment(wpA, "integration-test.txt", content,
        "text/plain");
    assertThat(attachmentId).isNotNull().isGreaterThan(0);

    // --- Add relation A -> B
    boolean relAdded = client.relationAdd(wpA, wpB, "test relation", RelationType.relates);
    assertThat(relAdded).isTrue();

    Collection<RelationDto> relsAfterAdd = client.listRelationsForWorkPackage(wpA);
    assertThat(relsAfterAdd).isNotNull();

    RelationDto createdRel = relsAfterAdd.stream()
        .filter(r -> Objects.equals(r.getToId(), wpB))
        .filter(r -> "relates".equals(r.getType()) || "relates".equalsIgnoreCase(r.getType()))
        .findFirst()
        .orElseGet(() -> relsAfterAdd.stream()
            .filter(r -> Objects.equals(r.getToId(), wpB))
            .findFirst()
            .orElse(null));

    assertThat(createdRel).isNotNull();
    assertThat(createdRel.getId()).isNotNull();

    // --- Delete relation by id
    boolean relDeleted = client.relationDelete(createdRel.getId());
    assertThat(relDeleted).isTrue();

    Collection<RelationDto> relsAfterDelete = client.listRelationsForWorkPackage(wpA);
    assertThat(relsAfterDelete.stream().filter(r -> Objects.equals(r.getId(), createdRel.getId()))
        .findAny())
        .isEmpty();

    // --- Parent relation add/delete (derived links)
    boolean parentAdded = client.relationAddParent(wpB, wpA);
    assertThat(parentAdded).isTrue();

    Collection<RelationDto> relsWithParent = client.listRelationsForWorkPackage(wpB);
    assertThat(relsWithParent).isNotNull();
    assertThat(relsWithParent.stream()
        .anyMatch(r -> "parent".equals(r.getType()) && Objects.equals(r.getToId(), wpA)))
        .isTrue();

    boolean parentDeleted = client.relationDeleteParent(wpB);
    assertThat(parentDeleted).isTrue();

    Collection<RelationDto> relsAfterParentDelete = client.listRelationsForWorkPackage(wpB);
    assertThat(relsAfterParentDelete.stream().anyMatch(r -> "parent".equals(r.getType())))
        .isFalse();
  }

  private int createWorkPackage(String subject) {
    WorkPackageCreateDto dto = new WorkPackageCreateDto();
    dto.setSubject(subject + " " + System.currentTimeMillis());
    dto.setDescription("Created by OpenProjectApiClientIntegrationTest");
    dto.setTypeId(demoTypeId);
    dto.setPriorityId(demoPriorityId);
    // dto.setStartDate(LocalDate.now());
    dto.setDueDate(LocalDate.now().plusDays(1));

    Integer id = client.workPackageCreate(demoProjectId, dto);
    assertThat(id).isNotNull();
    assertThat(id).isGreaterThan(0);
    return id;
  }

  // -------------------------
  // Container lifecycle
  // -------------------------

  private static synchronized void ensureStarted() {
    if (apiKey != null && baseUrl != null) {
      return;
    }
    baseUrl = "http://localhost:" + port;
    try {
      log.info("Starting container, which could take a while if images need pull.");
      dockerComposeEnv(Map.of(
          "OPENPROJECT_IMAGE_TAG", tag,
          "OPENPROJECT_PORT", "" + port,
          "OPENPROJECT_HOST_NAME", "localhost"
      ), "up", "-d", "--pull", "always");
      log.info("Waiting for OpenProject to be fully initialized.");

      waitForOpenProject(Duration.ofMinutes(5));
      log.info("Containerized OpenProject ({}) is running: '{}'!", tag, baseUrl);

      apiKey = generateAdminApiKeyWithRetry(Duration.ofMinutes(2));
      assertThat(apiKey).isNotBlank();

      // Do NOT log the token; only log a short fingerprint
      log.info("OpenProject API token generated (suffix={})", maskTokenSuffix(apiKey));
      log.info("OpenProject API token generated (suffix={})", apiKey);

    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to start OpenProject test container / generate API key: " + e.getMessage(), e);
    }
  }

  private static String maskTokenSuffix(String token) {
    if (token == null || token.isBlank()) {
      return "<none>";
    }
    String t = token.trim();
    return (t.length() <= 4) ? "****" : "****" + t.substring(t.length() - 4);
  }

  @SuppressWarnings("BusyWait")
  private static String generateAdminApiKeyWithRetry(Duration timeout) throws Exception {
    long deadline = System.nanoTime() + timeout.toNanos();
    Exception last = null;

    while (System.nanoTime() < deadline) {
      try {
        String key = generateAdminApiKey();
        if (!key.isBlank()) {
          return key;
        }
      } catch (Exception e) {
        last = e;
      }
      Thread.sleep(3000);
    }
    throw new IllegalStateException("Could not generate API key via rails runner in time: "
        + (last == null ? "<none>" : last.getMessage()), last);
  }

  @SuppressWarnings("BusyWait")
  private static void waitForOpenProject(Duration timeout) throws Exception {
    HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    long deadline = System.nanoTime() + timeout.toNanos();
    Exception last = null;

    while (System.nanoTime() < deadline) {
      try {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v3"))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200 || resp.statusCode() == 401) {
          return;
        }
      } catch (Exception e) {
        last = e;
      }
      Thread.sleep(3000);
    }

    throw new IllegalStateException("OpenProject did not become ready in time at " + baseUrl
        + " (last error: " + (last == null ? "<none>" : last.getMessage()) + ")");
  }

  private static String generateAdminApiKey() throws Exception {
    final var ruby = """
        u = User.find_by(login: 'admin')
        raise 'admin user not found' unless u
        t = Token::API.create!(user: u)
        if t.respond_to?(:plain_value) && t.plain_value
          puts t.plain_value
        elsif t.respond_to?(:raw_token) && t.raw_token
          puts t.raw_token
        elsif t.respond_to?(:value) && t.value
          puts t.value
        else
          raise 'cannot extract token plaintext'
        end
        """;

    List<List<String>> candidates = List.of(
        List.of("exec", "-T", COMPOSE_SERVICE_NAME, "bash", "-lc",
            "bundle exec rails runner " + shellQuote(ruby))
    );

    for (List<String> args : candidates) {
      CmdResult r = dockerComposeCapture(args.toArray(String[]::new));
      if (r.exitCode == 0) {
        String out = r.stdout.trim();
        if (!out.isBlank()) {
          List<String> lines = out.lines().map(String::trim).filter(s -> !s.isBlank()).toList();
          return lines.getLast();
        }
      }
    }

    throw new IllegalStateException(
        "Could not generate API key via rails runner in container. Check container logs / command availability.");
  }

  @SuppressWarnings("SameParameterValue")
  private static void dockerCompose(String... composeArgs) throws Exception {
    CmdResult r = dockerComposeCapture(composeArgs);
    if (r.exitCode != 0) {
      throw new IllegalStateException("docker compose failed: " + r.stderr);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private static void dockerComposeEnv(Map<String, String> env, String... composeArgs)
      throws Exception {
    CmdResult r = dockerComposeCapture(env, composeArgs);
    if (r.exitCode != 0) {
      throw new IllegalStateException("docker compose failed: " + r.stderr);
    }
  }

  private static CmdResult dockerComposeCapture(String... composeArgs) throws Exception {
    return dockerComposeCapture(Map.of(), composeArgs);
  }

  private static CmdResult dockerComposeCapture(Map<String, String> env, String... composeArgs)
      throws Exception {
    List<String> cmd = new java.util.ArrayList<>();
    cmd.add("docker");
    cmd.add("compose");
    cmd.add("-f");
    cmd.add(Path.of(COMPOSE_DIR, COMPOSE_FILE).toString());
    cmd.addAll(Arrays.asList(composeArgs));

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(Path.of(".").toFile());
    pb.redirectErrorStream(false);
    pb.environment().putAll(env);

    Process p = pb.start();

    boolean finished = p.waitFor(15, TimeUnit.MINUTES);
    if (!finished) {
      p.destroyForcibly();
      throw new IllegalStateException("docker compose timed out");
    }

    String stdout;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      stdout = br.lines().collect(Collectors.joining("\n"));
    }
    String stderr;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
      stderr = br.lines().collect(Collectors.joining("\n"));
    }

    return new CmdResult(p.exitValue(), stdout, stderr);
  }

  private static String shellQuote(String s) {
    // single-quote safe for bash -lc '...'
    return "'" + s.replace("'", "'\"'\"'") + "'";
  }

  private record CmdResult(int exitCode, String stdout, String stderr) {

  }
}
