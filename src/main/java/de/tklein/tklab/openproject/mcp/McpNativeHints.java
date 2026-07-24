package de.tklein.tklab.openproject.mcp;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.ai.mcp.annotation.context.DefaultMetaProvider;

class McpNativeHints implements RuntimeHintsRegistrar {

  private static final TypeReference CAFFEINE_SSW = TypeReference.of(
      "com.github.benmanes.caffeine.cache.SSW");

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    hints.reflection()
        .registerType(DefaultMetaProvider.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    hints.reflection()
        .registerType(CAFFEINE_SSW, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.ACCESS_DECLARED_FIELDS);
  }
}
