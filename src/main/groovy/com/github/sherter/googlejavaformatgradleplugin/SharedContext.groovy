package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.Project

class SharedContext {

    private final Project project
    private FileStateHandler fileStateHandler
    private Formatter formatter

    SharedContext(Project project) {
        this.project = Objects.requireNonNull(project)
    }

    synchronized FileStateHandler fileStateHandler() {
        if (fileStateHandler == null) {
            String buildCacheSubDir = "google-java-format/${GoogleJavaFormatPlugin.PLUGIN_VERSION}"
            GoogleJavaFormatExtension extension = project.extensions.findByType(GoogleJavaFormatExtension)
            fileStateHandler = new FileStateHandler(
                    project.projectDir,
                    new File(project.buildDir, buildCacheSubDir),
                    extension.toolVersion)
            fileStateHandler.load()
            project.gradle.buildFinished { fileStateHandler.flush() }
        }
        return fileStateHandler
    }

    synchronized Formatter formatter() {
        if (formatter == null) {
            GoogleJavaFormatExtension extension = project.extensions.findByType(GoogleJavaFormatExtension)
            formatter = new FormatterFactory(project, project.logger).create(extension.toolVersion)
        }
        return formatter
    }
}
