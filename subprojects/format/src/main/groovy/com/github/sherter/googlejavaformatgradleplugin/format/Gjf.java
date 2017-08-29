package com.github.sherter.googlejavaformatgradleplugin.format;

import com.google.common.collect.ImmutableList;

/** Static factory method for creating new {@link Formatter}s. */
public class Gjf {

  public static final String GROUP_ID = "com.google.googlejavaformat";
  public static final String ARTIFACT_ID = "google-java-format";

  public static final ImmutableList<String> SUPPORTED_VERSIONS =
      ImmutableList.of("1.0", "1.1", "1.2", "1.3", "1.4");

  /**
   * Constructs a new formatter that delegates to <a
   * href="https://github.com/google/google-java-format">google-java-format</a>.
   *
   * @param classLoader load {@code google-java-format} classes from this {@code ClassLoader}
   * @param config configure the formatter according to this configuration
   * @throws ReflectiveOperationException if the requested {@code Formatter} cannot be constructed
   */
  public static Formatter newFormatter(ClassLoader classLoader, Configuration config)
      throws ReflectiveOperationException {
    return newFormatterFactory(classLoader, config).create();
  }

  private static FormatterFactory newFormatterFactory(
      ClassLoader classLoader, Configuration config) {
    switch (config.version) {
      case "1.0":
        return new OneDotZeroFactory(classLoader, config);
      case "1.1":
        return new OneDotOneFactory(classLoader, config);
      default:
        return new OneDotOneFactory(classLoader, config);
    }
  }
}
