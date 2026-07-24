package de.tklein.tklab.openproject.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(McpNativeHints.class)
public class SpringOpenprojectMcpServerApplication {

  static void main(String[] args) {
    SpringApplication.run(SpringOpenprojectMcpServerApplication.class, args);
  }

}
