package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class GoogleJavaFormatPlugin implements Plugin<Project> {
    static final String CONFIGURATION_NAME = "googleJavaFormat"
    static final String EXTENSION_NAME = "googleJavaFormat"
    static final String DEFAULT_TASK_NAME = "googleJavaFormat"

    static final String GOOGLEJAVAFORMAT_GROUPID = "com.google.googlejavaformat"
    static final String GOOGLEJAVAFORMAT_ARTIFACTID = "google-java-format"
    static final String GOOGLEJAVAFORMAT_DEFAULT_VERSION = "0.1-alpha"

    private Project project
    private GoogleJavaFormatPluginExtension extension
    private Configuration config
    private GoogleJavaFormatTask defaultTask
    private FileStateHandler fileStateHandler

    void apply(Project project) {
        this.project = project
        createProjectExtension()
        createConfiguration()
        createDefaultFormatTask()

        project.afterEvaluate {
            addDependencyForGoogleJavaFormat()
            defineInputsForDefaultFormatTask()
            createAndInjectFileStateHandler()
            this.fileStateHandler.load()
            excludeUpToDateInputs()
            it.gradle.buildFinished {
                this.fileStateHandler.flush()
            }
        }
    }

    private void createProjectExtension() {
        this.extension = this.project.extensions.create(EXTENSION_NAME, GoogleJavaFormatPluginExtension)
    }

    private void createConfiguration() {
        this.config = this.project.configurations.create(CONFIGURATION_NAME) {
            visible = false
            transitive = true
        }
    }

    private void createDefaultFormatTask() {
        this.defaultTask = this.project.tasks.create(DEFAULT_TASK_NAME, GoogleJavaFormatTask)
    }

    private void addDependencyForGoogleJavaFormat() {
        def dependency = this.project.dependencies.create(
                group: GOOGLEJAVAFORMAT_GROUPID,
                name: GOOGLEJAVAFORMAT_ARTIFACTID,
                version: this.extension.toolVersion
        )
        this.config.dependencies.add(dependency)
    }

    private void defineInputsForDefaultFormatTask() {
        if (this.project.hasProperty('sourceSets')) {
            this.project.sourceSets.all { sourceSet ->
                this.defaultTask.source(sourceSet.java)
            }
        }
    }

    private void createAndInjectFileStateHandler() {
        String pluginVersion = getClass().getClassLoader().getResourceAsStream('VERSION').text.trim()
        String buildCacheSubdir = "google-java-format/$pluginVersion"
        this.fileStateHandler = new FileStateHandler(
                this.project.projectDir,
                new File(this.project.buildDir, buildCacheSubdir),
                this.extension.toolVersion)
        this.project.tasks.withType(GoogleJavaFormatTask) { formatTask ->
            formatTask.fileStateHandler = this.fileStateHandler
        }
    }

    private void excludeUpToDateInputs() {
        this.project.tasks.withType(GoogleJavaFormatTask) { formatTask ->
            formatTask.exclude { fileTreeElement ->
                return this.fileStateHandler.isUpToDate(fileTreeElement.file)
            }
        }
    }
}
