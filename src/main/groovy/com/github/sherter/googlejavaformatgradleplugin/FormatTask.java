package com.github.sherter.googlejavaformatgradleplugin;

import org.gradle.api.tasks.SourceTask;

abstract class FormatTask extends SourceTask {

  protected SharedContext sharedContext;

  abstract void accept(TaskConfigurator configurator);

  boolean hasSources() {
    return !source.isEmpty();
  }
}
