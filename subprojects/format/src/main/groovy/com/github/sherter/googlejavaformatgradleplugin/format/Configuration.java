package com.github.sherter.googlejavaformatgradleplugin.format;

public final class Configuration {
  public final String version;
  public final Style style;
  public final boolean orderImports;

  public Configuration(String version, Style style, boolean orderImports) {
    this.version = version;
    this.style = style;
    this.orderImports = orderImports;
  }
}
