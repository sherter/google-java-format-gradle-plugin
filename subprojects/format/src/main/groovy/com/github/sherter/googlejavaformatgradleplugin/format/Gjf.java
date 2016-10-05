package com.github.sherter.googlejavaformatgradleplugin.format;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static factory method for creating new {@link Formatter}s.
 */
public class Gjf {

  public static final String GROUP_ID = "com.google.googlejavaformat";
  public static final String ARTIFACT_ID = "google-java-format";

  public static final ImmutableList<String> SUPPORTED_VERSIONS = ImmutableList.of("1.0");

  /**
   * Constructs a new formatter that delegates to <a
   * href="https://github.com/google/google-java-format">google-java-format</a>.
   *
   * @param classLoader load {@code google-java-format} classes from this {@code ClassLoader}
   * @param version load this version of {@code google-java-format}
   * @param options configure the returned {@code Formatter} with these options
   *
   * @throws IllegalArgumentException if the requested {@code Formatter} cannot be constructed
   * <p>
   * Possible reasons are:
   * </p>
   * <ul>
   * <li>The provided {@code classLoader} fails to provide a required class</li>
   * <li>A provided class is incompatible with the requested {@code version}</li>
   * <li>One or multiple {@code options} are not supported by the given {@code version} of
   * {@code google-java-format}</li>
   * <li>Options are conflicting</li>
   * </ul>
   * @throws NullPointerException if any of the given parameters is {@code null}
   */
  public static Formatter newFormatter(
      ClassLoader classLoader, String version, FormatterOption... options) {
    checkClassLoader(checkNotNull(classLoader), checkNotNull(version));
    FormatterFactory factory = createFactory(classLoader, version, checkNotNull(options));
    try {
      return factory.create(options);
    } catch (Throwable t) {
      if (t instanceof IllegalArgumentException) {
        throw t;
      }
      throw new IllegalArgumentException(t);
    }
  }

  public static Formatter newFormatter(ClassLoader classLoader, Configuration config) throws ReflectiveOperationException {
    return newFormatterFactory(classLoader, config).create();
  }

  private static FormatterFactory newFormatterFactory(ClassLoader classLoader, Configuration config) {
    switch (config.version) {
      case "1.0":
        return new OneDotZeroFactory(classLoader, config);
      default:
        return new OneDotZeroFactory(classLoader, config);
    }
  }

  private static void checkClassLoader(ClassLoader classLoader, String version) {
    switch (version) {
      case "0.1-alpha":
        checkStaticVersionFieldValueEquals(classLoader, "0.dev");
        break;
      case "1.0":
        checkStaticVersionFieldValueEquals(classLoader, "1.0");
        break;
      default:
        // unsupported version; don't check classloader and just try to proceed
    }
  }

  private static void checkStaticVersionFieldValueEquals(ClassLoader classLoader, String expected) {
    try {
      Class<?> versionClass =
          classLoader.loadClass("com.google.googlejavaformat.java.GoogleJavaFormatVersion");
      Object actualVersion = versionClass.getField("VERSION").get(null);
      if (!expected.equals(actualVersion)) {
        throw new IllegalArgumentException("Invalid ClassLoader");
      }
    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalArgumentException("Invalid ClassLoader", e);
    }
  }

  private static FormatterFactory createFactory(
      ClassLoader classLoader, String version, FormatterOption[] options) {
    if (options.length == 0) {
      return new NoOptionsFactory(classLoader);
    }
    if (version.equals("0.1-alpha")) {
      throw new IllegalArgumentException("Unsupported options: " + Arrays.toString(options));
    }
    return new OptionsFactory(classLoader);
  }
}
