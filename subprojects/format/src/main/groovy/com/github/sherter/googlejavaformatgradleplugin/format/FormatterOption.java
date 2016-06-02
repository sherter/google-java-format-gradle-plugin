package com.github.sherter.googlejavaformatgradleplugin.format;

import com.google.common.collect.ImmutableList;

/**
 * A {@code FormatterOption} configures a {@link Formatter} to behave in a certain way.
 *
 * <p>Different versions of <a href="https://github.com/google/google-java-format">google-java-format</a>
 * are configured differently. In order to provide a unified configuration interface for all versions,
 * this class contains (an abstraction) of every option that is supported by at least one version of
 * {@code google-java-format}.</p>
 *
 */
public enum FormatterOption {

  /** Supported by {@code google-java-format} version {@code 1.0}. */
  AOSP_STYLE("1.0"),

  /** Supported by {@code google-java-format} version {@code 1.0}. */
  GOOGLE_STYLE("1.0"),
  NO_JAVADOC_FORMATTER("1.0"),
  ECLIPSE_JAVADOC_FORMATTER("1.0"),
  SORT_IMPORTS("1.0");

  public final ImmutableList<String> supportedVersions;

  FormatterOption(String... supportedVersions) {
    this.supportedVersions = ImmutableList.copyOf(supportedVersions);
  }
}
