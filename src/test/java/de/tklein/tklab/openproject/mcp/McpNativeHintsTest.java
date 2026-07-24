package de.tklein.tklab.openproject.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.ai.mcp.annotation.context.DefaultMetaProvider;

class McpNativeHintsTest {

  @Test
  void registersDefaultMetaProviderConstructorForNativeImages() {
    RuntimeHints hints = new RuntimeHints();

    new McpNativeHints().registerHints(hints, getClass().getClassLoader());

    assertThat(
        RuntimeHintsPredicates.reflection().onType(DefaultMetaProvider.class).test(hints)).isTrue();
  }
}
