package com.github.sherter.googlejavaformatgradleplugin;

import com.github.sherter.googlejavaformatgradleplugin.format.Style;
import com.google.auto.value.AutoValue;
import org.gradle.api.Nullable;

@AutoValue
abstract class FormatterOptions {

  static FormatterOptions create(@Nullable String version, @Nullable Style style) {
    return new AutoValue_FormatterOptions(version, style);
  }

  @Nullable
  abstract String version();

  @Nullable
  abstract Style style();
}
