package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import dagger.Module;
import dagger.Provides;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    try (InputStream is = getClass().getResourceAsStream("/VERSION")) {
      return CharStreams.toString(new InputStreamReader(is, StandardCharsets.UTF_8)).trim();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
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

  @Provides
  @Named("settings")
  Path provideSettingsPath(@Named("output path") Path outputPath) {
    return outputPath.resolve("settings.txt");
  }
}
