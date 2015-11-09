package com.github.sherter.googlejavaformat

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class GoogleJavaFormatPlugin implements Plugin<Project> {
    static final String CONFIGURATION_NAME = "googleJavaFormat"
    static final String EXTENSION_NAME = "googleJavaFormat"
    static final String TASK_NAME = "googleJavaFormat"

    static final String GOOGLEJAVAFORMAT_GROUPID = "com.google.googlejavaformat"
    static final String GOOGLEJAVAFORMAT_ARTIFACTID = "google-java-format"

    private Project project
    private GoogleJavaFormatPluginExtension extension
    private Configuration config
    private GoogleJavaFormatTask task


    void apply(Project project) {
        this.project = project
        createProjectExtension()
        createConfiguration()
        createFormatTask()

        project.afterEvaluate {
            addDependencyForGoogleJavaFormat()
            configureFormatTask()
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

    private void createFormatTask() {
        this.task = this.project.tasks.create(TASK_NAME, GoogleJavaFormatTask)
    }

    private void addDependencyForGoogleJavaFormat() {
        def dependency = this.project.dependencies.create(
                group: GOOGLEJAVAFORMAT_GROUPID,
                name: GOOGLEJAVAFORMAT_ARTIFACTID,
                version: this.extension.toolVersion
        )
        this.config.dependencies.add(dependency)
    }

    private void configureFormatTask() {
        if (!this.project.properties.containsKey('sourceSets')) {
            return
        }
        this.project.sourceSets.all { sourceSet ->
            sourceSet.java.files.each { javaFile ->
                this.task.inputs.file(javaFile)
                this.task.outputs.file(javaFile)
            }
        }
        // TODO(sherter): add build file to task inputs (rerun if user changes 'toolVersion')
        def urls = config.files.collect {
            it.toURI().toURL()
        }
        def classLoader = new URLClassLoader(urls as URL[], ClassLoader.getSystemClassLoader())
        this.task.formatter = classLoader.loadClass('com.google.googlejavaformat.java.Formatter').newInstance()
    }

}
