# spring-openproject-mcp-server

MCP server to manage OpenProject work-packages realized in Java.
The server acts as a proxy to your OpenProject API. 
The user's' OpenProject API token is used for authentication (not stored in the container!).


## Open project compatibility
Tested against OpenProject 14,15,16.6,17.5


## Get started (using LM Studio)
1. Launch the Docker container
  `docker run -d -p 0.0.0.0:8080:8080 -e OPENPROJECT_URL=https://${$yourOpenProject} --tmpfs /tmp spring-openproject-mcp-server:latest`
2. Run LM Studio and choose an appropriate model for your project domain. _qwen/qwen3-coder-30b_ works well to create technical epics and user stories. For simple translations and text refinements, a smaller model is enough.
3. Configure LM Studio's `mcp.json` file below *"mcpServers":{...}* and set the API token for the project to use (see below)
4. If you see *"mcp/openproject-mcp"* on the right side, below Integrations start prompting.
5. Examples:
   1. Prompt: "List all projects in OpenProject using mcp/openproject-mcp."
   2. Prompt: "Use mcp/openproject-mcp to get work-packages from project id=10 and analyze the project."
   3. Prompt: "Use mcp/openproject-mcp and translate all epics in the project 'MyApp' to German."
   4. Prompt: "You are a precise Agile assistant. Help with the specification of the project. Use mcp/openproject-mcp and project id=10. Add INVEST criteria. Add Gherkin acceptance criteria. Add Definition of Done (checklist). Add Risks & Mitigations. Goals & measurable KPIs/OKRs (SMART, incl. target values)"
   5. Prompt: "Create 10 Epics for a new WebApp which does the following: ... Use mcp/openproject-mcp and project 'NewWebApp'."


## Docker

Find the latest image at [https://hub.docker.com/r/tmskln/spring-openproject-mcp-server](https://hub.docker.com/r/tmskln/spring-openproject-mcp-server)

### build
```bash
# convert java SBOM to optimized OCI format
docker run --rm \
  -v "./target/classes/META-INF/sbom:/work"  --platform linux/amd64 \
  cyclonedx/cyclonedx-cli \
  convert \
  --input-file /work/application.cdx.json \
  --output-file /work/application.spdx.json \
  --output-format spdxjson
  
PKG_VERSION="dev" && docker build \
 -f docker/Dockerfile \
 -t spring-openproject-mcp-server:${PKG_VERSION} \
 --attest type=sbom,generator=docker/scout-sbom-indexer:latest \
 --label org.opencontainers.image.build-date=$(date -u +"%Y-%m-%dT%H:%M:%SZ") \
 --label org.opencontainers.image.revision=$(git rev-parse HEAD) \
 --label org.opencontainers.image.version=${PKG_VERSION} \
 --platform linux/amd64,linux/arm64 \
 . 
 ```

### run
```bash
docker run -d -p 0.0.0.0:8080:8080 -e OPENPROJECT_URL=https://${$yourOpenProject} --tmpfs /tmp spring-openproject-mcp-server
```
```json
{
  "mcpServers": {
    "openproject-mcp": {
      "url": "http://127.0.0.1:8080/sse",
      "headers": {
        "Authorization": "Bearer {YourOpenProjectApiToken}"
      }
    }
  }
}
```

If you want to control the OpenProject server from the MCP-client or run against multiple OpenProject servers set start the container with ```-e OPENPROJECT_ALLOW_HEADER_BASE_URL=true``` and set the server URL in MCP config:
```json
{
  "Authorization": "Bearer {YourOpenProjectApiToken}",
  "X-OpenProject-Base-Url": "https://${$yourOpenProjexct}"
}
```

## Integration Tests
```bash
mvn test -Dopenproject.container.tag=17.5.1 -Dopenproject.container.port=18080
```
Mind to remove volumes between tests 


## TODO
- MCP OpenProject with OTEL
- use results after patch JSON
- add kubernetes deployment+service (helm?)


## SSE vs. Streamable

spring.ai.mcp.server.protocol=SSE
spring.ai.mcp.server.protocol=STREAMABLE

Spring creates one MCP server bean with one transport:

- SSE protocol → exposes:
  - GET /sse for server → client streaming
  - POST /mcp/message for client → server communication
- STREAMABLE protocol → exposes a single bidirectional endpoint (default /mcp) based on the new MCP streamable HTTP spec.

Because the transport defines the wiring, endpoints, and message routing, Spring cannot bind two protocols simultaneously.


## FAQ

- Why wasn't a generated OpenAPI client used, but JSONNode value mapping with MapStruct?
  - The first approach was using a generated OpenAPI client, but there were many issues:
    - code generation with org.openapitools:openapi-generator-maven-plugin wasn't totally clean and needed lots of manual corrections.
    - client is very strict and would fail minimal spec changes
    - Spec lacks values, e.g. 'storyPoints' and manually enhancing generated code is not applicable
    - compatibility to a wider range of versions can better be realized with direct value mapping and integration tests against multiple versions of OpenProject


## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.
