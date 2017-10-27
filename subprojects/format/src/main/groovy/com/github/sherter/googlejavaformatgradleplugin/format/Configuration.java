package com.github.sherter.googlejavaformatgradleplugin.format;

public final class Configuration {
  public final String version;
  public final Style style;
  public final boolean sortImports;

  public Configuration(String version, Style style, boolean sortImports) {
    this.version = version;
    this.style = style;
    this.sortImports = sortImports;
  }
}
