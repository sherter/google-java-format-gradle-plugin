package com.github.sherter.googlejavaformatgradleplugin.format;

/**
 * Represents the common functionality that all versions of
 * <a href="https://github.com/google/google-java-format">google-java-format</a> provide.
 *
 * This abstraction makes it easy to use different versions of {@code google-java-format} uniformly.
 */
public interface Formatter {
  String format(String source) throws FormatterException;
}
