package de.tklein.tklab.openproject.mcp.util;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for map creation with null-value filtering.
 */
public class PatchMap {

  public enum Nullable {
    ALLOW_NULL_VALUES, DROP_NULL_VALUES
  }

  /**
   * Creates a map with the provided key-value pairs, not adding any entries where the value is
   * null. This is similar to Map.of() but filters out null values.
   *
   * @param keysAndValues alternating keys and values (key1, value1, key2, value2, ...)
   * @return a new map with non-null values
   * @throws IllegalArgumentException if the number of arguments is odd
   */
  public static Map<String, Object> of(@Nonnull PatchMap.Nullable nullable,
      Object... keysAndValues) {

    if (keysAndValues.length % 2 != 0) {
      throw new IllegalArgumentException("Number of  arguments must be even");
    }

    Map<String, Object> result = HashMap.newHashMap(keysAndValues.length / 2); // NO PITEST

    for (int i = 0; i < keysAndValues.length; i += 2) {
      String key = (String) keysAndValues[i];
      Object value = keysAndValues[i + 1];

      if (nullable == Nullable.ALLOW_NULL_VALUES || value != null) {
        result.put(key, value);
      }
    }

    return result;
  }
}
