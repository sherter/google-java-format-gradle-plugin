package com.github.sherter.googlejavaformatgradleplugin;

import com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import groovy.transform.Immutable;
import org.gradle.api.Nullable;

@AutoValue
abstract class FormatterOptions {

  static FormatterOptions create(@Nullable String version, ImmutableSet<FormatterOption> options) {
    return new AutoValue_FormatterOptions(version, options);
  }

  @Nullable
  abstract String version();

  abstract ImmutableSet<FormatterOption> options();
}
