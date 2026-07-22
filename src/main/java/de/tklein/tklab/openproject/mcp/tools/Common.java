package de.tklein.tklab.openproject.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class Common {

  /*
   * Logging is only for MCP clients, we have only an MCP server here!
   */
  @McpLogging(clients = "openproject-mcp-server")
  public void handleLoggingMessage(McpSchema.LoggingMessageNotification notification) {
    log.info("Received log: {} - {}", notification.level(), notification.data());
  }

}
