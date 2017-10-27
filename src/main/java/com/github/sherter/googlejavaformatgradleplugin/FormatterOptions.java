package com.github.sherter.googlejavaformatgradleplugin;

import com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nullable;

@AutoValue
abstract class FormatterOptions {

  static FormatterOptions create(
      @Nullable String version, ImmutableSet<FormatterOption> options, boolean orderImports) {
    return new AutoValue_FormatterOptions(version, options, orderImports);
  }

  @Nullable
  abstract String version();

  abstract ImmutableSet<FormatterOption> options();

  abstract boolean orderImports();
}
