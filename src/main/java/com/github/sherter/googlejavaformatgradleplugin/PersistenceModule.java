package com.github.sherter.googlejavaformatgradleplugin;

import dagger.Module;
import dagger.Provides;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import javax.inject.Named;
import java.nio.file.Path;
import java.nio.file.Paths;

@Module
class PersistenceModule {

  private final Project project;

  PersistenceModule(Project project) {
    this.project = project;
  }

  @Provides
  Logger provideLogger() {
    return project.getLogger();
  }

  @Provides
  @Named("plugin version")
  String providePluginVersion() {
    // TODO(sherter): compute this dynamically
    return "0.3";
  }

  @Provides
  @Named("base path")
  Path provideBasePath() {
    return project.getProjectDir().toPath();
  }

  @Provides
  @Named("output path")
  Path provideOutputPath(@Named("plugin version") String pluginVersion) {
    Path buildDir = project.getBuildDir().toPath();
    return buildDir.resolve(Paths.get("google-java-format", pluginVersion));
  }

  @Provides
  @Named("storage")
  Path provideStoragePath(@Named("output path") Path outputPath) {
    return outputPath.resolve("fileStates.txt");
  }
}
