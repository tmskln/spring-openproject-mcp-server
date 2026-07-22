package de.tklein.tklab.openproject.mcp.openproject.client;

import tools.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OpenProjectRestOperations {

  public static final String APPLICATION_HAL_JSON = "application/hal+json";
  private final OpenProjectRestClientFactory restClientFactory;

  private RestClient op() {
    return restClientFactory.createClient();
  }

  public JsonNode getJson(String uriTemplate, Object... uriVariables) {
    return op().get()
        .uri(uriTemplate, uriVariables)
        .accept(MediaType.parseMediaType(APPLICATION_HAL_JSON))
        .retrieve()
        .body(JsonNode.class);
  }

  public JsonNode postJson(String uriTemplate, String jsonBody, Object... uriVariables) {
    return op().post()
        .uri(uriTemplate, uriVariables)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.parseMediaType(APPLICATION_HAL_JSON))
        .body(jsonBody)
        .retrieve()
        .body(JsonNode.class);
  }

  public JsonNode patchJson(String uriTemplate, String jsonBody, Object... uriVariables) {
    return op().patch()
        .uri(uriTemplate, uriVariables)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.parseMediaType(APPLICATION_HAL_JSON))
        .body(jsonBody)
        .retrieve()
        .body(JsonNode.class);
  }

  public void delete(String uriTemplate, Object... uriVariables) {
    op().delete()
        .uri(uriTemplate, uriVariables)
        .accept(MediaType.parseMediaType(APPLICATION_HAL_JSON))
        .retrieve()
        .toBodilessEntity();
  }

  public JsonNode postMultipart(String uriTemplate, MultiValueMap<String, Object> body,
      Object... uriVariables) {
    return op().post()
        .uri(uriTemplate, uriVariables)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(body)
        .retrieve()
        .body(JsonNode.class);
  }

  public MultiValueMap<String, Object> buildMultipartAttachmentBody(String fileName,
      byte[] fileContent, String fileContentType) {
    var metadataJson = Map.of("fileName", fileName);
    var metadataHeaders = new HttpHeaders();
    metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
    var metadataPart = new HttpEntity<>(metadataJson, metadataHeaders);

    var mediaType = (fileContentType == null || fileContentType.isBlank())
        ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(fileContentType);

    var fileResource = new ByteArrayResource(fileContent) {
      @Override
      public String getFilename() {
        return fileName;
      }
    };

    var fileHeaders = new HttpHeaders();
    fileHeaders.setContentType(mediaType);
    var filePart = new HttpEntity<>(fileResource, fileHeaders);

    var body = new LinkedMultiValueMap<String, Object>();
    body.add("metadata", metadataPart);
    body.add("file", filePart);
    return body;
  }

  public <T> List<T> mapEmbeddedElements(JsonNode result, Function<JsonNode, T> mapper) {
    var elements = result != null ? result.path("_embedded").path("elements") : null;
    if (elements == null || !elements.isArray()) {
      return Collections.emptyList();
    }
    return StreamSupport.stream(elements.spliterator(), false)
        .map(mapper)
        .toList();
  }
}