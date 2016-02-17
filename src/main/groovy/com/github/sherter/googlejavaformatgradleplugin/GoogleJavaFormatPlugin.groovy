package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeChecked
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileTreeElement

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
            createAndInjectFormatterFactory()
            this.fileStateHandler.load()
            excludeUpToDateInputs()
            project.gradle.buildFinished {
                this.fileStateHandler.flush()
            }
        }
    }

    private void createAndInjectFormatterFactory() {
        def formatterFactory = new FormatterFactory(config, extension.toolVersion)
        this.project.tasks.withType(GoogleJavaFormat) { GoogleJavaFormat formatTask ->
            formatTask.setFormatterFactory(formatterFactory)
        }
    }

    private void createProjectExtension() {
        // TODO (sherter): remove cast when future groovy version correctly infer type
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

    private void addDependencyForGoogleJavaFormat() {
        def dependency = this.project.dependencies.create(
                group: GOOGLEJAVAFORMAT_GROUPID,
                name: GOOGLEJAVAFORMAT_ARTIFACTID,
                version: this.extension.toolVersion
        )
        this.config.dependencies.add(dependency)
    }

    @TypeChecked(SKIP)
    private void defineInputsForDefaultFormatTask() {
        if (this.project.hasProperty('sourceSets')) {
            this.project.sourceSets.all { sourceSet ->
                this.defaultTask.source(sourceSet.java)
            }
        }
    }

    private void createAndInjectFileStateHandler() {
        // workaround; reading resources fails sometimes
        // see https://discuss.gradle.org/t/getresourceasstream-returns-null-in-plugin-in-daemon-mode/2385
        new URLConnection(new URL("file:///")) {
            {
                setDefaultUseCaches(false)
            }
            @Override
            void connect() throws IOException {
            }
        }
        String pluginVersion = getClass().getResourceAsStream('/VERSION').text.trim()
        String buildCacheSubdir = "google-java-format/$pluginVersion"
        this.fileStateHandler = new FileStateHandler(
                this.project.projectDir,
                new File(this.project.buildDir, buildCacheSubdir),
                this.extension.toolVersion)
        this.project.tasks.withType(GoogleJavaFormat) { GoogleJavaFormat formatTask ->
            formatTask.setFileStateHandler(this.fileStateHandler)
        }
    }

    private void excludeUpToDateInputs() {
        this.project.tasks.withType(GoogleJavaFormat) { GoogleJavaFormat formatTask ->
            formatTask.exclude { FileTreeElement e ->
                return this.fileStateHandler.isUpToDate(e.file)
            }
        }
    }
}
