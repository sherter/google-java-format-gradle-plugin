package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeChecked
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.SourceSet

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class GoogleJavaFormatPlugin implements Plugin<Project> {
    private static final String CONFIGURATION_NAME = "googleJavaFormat"
    private static final String EXTENSION_NAME = "googleJavaFormat"
    private static final String DEFAULT_TASK_NAME = "googleJavaFormat"

    private static final String GOOGLEJAVAFORMAT_GROUPID = "com.google.googlejavaformat"
    private static final String GOOGLEJAVAFORMAT_ARTIFACTID = "google-java-format"
    @PackageScope static final String GOOGLEJAVAFORMAT_DEFAULT_VERSION = "0.1-alpha"

    private Project project
    private GoogleJavaFormatExtension extension
    private Configuration config
    private GoogleJavaFormat defaultTask

    @Override
    void apply(Project project) {
        this.project = project
        createExtension()
        createConfiguration()
        createDefaultFormatTask()

        project.afterEvaluate {
            configureConfiguration()
            configureTasks()
        }
    }

    private void createExtension() {
        // TODO (sherter): remove cast when future groovy versions correctly infer type
        this.extension = (GoogleJavaFormatExtension) this.project.extensions.create(EXTENSION_NAME, GoogleJavaFormatExtension)
    }

    private void createConfiguration() {
        this.config = this.project.configurations.create(CONFIGURATION_NAME) { Configuration c ->
            c.visible = false
            c.transitive = true
        }
    }

    private void createDefaultFormatTask() {
        this.defaultTask = this.project.tasks.create(DEFAULT_TASK_NAME, GoogleJavaFormat)
    }

    private void configureConfiguration() {
        def dependency = this.project.dependencies.create(
                group: GOOGLEJAVAFORMAT_GROUPID,
                name: GOOGLEJAVAFORMAT_ARTIFACTID,
                version: this.extension.toolVersion
        )
        this.config.dependencies.add(dependency)
    }

    private void configureTasks() {
        def fileStateHandler = createFileStateHandler()
        def formatterFactory = new FormatterFactory(config, extension.toolVersion)
        fileStateHandler.load()
        project.gradle.buildFinished {
            fileStateHandler.flush()
        }
        project.tasks.withType(GoogleJavaFormat) { GoogleJavaFormat task ->
            task.setFileStateHandler(fileStateHandler)
            task.setFormatterFactory(formatterFactory)
            task.exclude { FileTreeElement f -> fileStateHandler.isUpToDate(f.file) }
        }
        addDefaultInputsToDefaultFormatTask()
    }

    private FileStateHandler createFileStateHandler() {
        // workaround; reading resources fails sometimes
        // see https://discuss.gradle.org/t/getresourceasstream-returns-null-in-plugin-in-daemon-mode/2385
        new URLConnection(new URL("file:///")) {
            { setDefaultUseCaches(false) }
            void connect() throws IOException {}
        }
        String pluginVersion = getClass().getResourceAsStream('/VERSION').text.trim()
        String buildCacheSubDir = "google-java-format/$pluginVersion"
        return new FileStateHandler(
                this.project.projectDir,
                new File(this.project.buildDir, buildCacheSubDir),
                this.extension.toolVersion)
    }

    @TypeChecked(SKIP)
    private void addDefaultInputsToDefaultFormatTask() {
        if (this.project.hasProperty('sourceSets')) {
            this.project.sourceSets.all { SourceSet sourceSet ->
                this.defaultTask.source(sourceSet.java)
            }
        }
    }
}
