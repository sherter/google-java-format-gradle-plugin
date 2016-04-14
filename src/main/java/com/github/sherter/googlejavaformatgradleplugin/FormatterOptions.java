package com.github.sherter.googlejavaformatgradleplugin;

import com.google.auto.value.AutoValue;
import org.gradle.api.Nullable;

@AutoValue
abstract class FormatterOptions {

  static FormatterOptions create(@Nullable String version) {
    return new AutoValue_FormatterOptions(version);
  }

  @Nullable
  abstract String version();
}
