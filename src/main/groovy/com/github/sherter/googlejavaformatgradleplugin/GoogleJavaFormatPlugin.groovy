package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class GoogleJavaFormatPlugin implements Plugin<Project> {

    /** Supported versions of google-java-format in <em>descending order</em>. */
    static final List<String> GOOGLEJAVAFORMAT_VERSIONS = ['0.1-alpha'].asImmutable()

    static final String PLUGIN_VERSION
    static {
        // workaround; reading resources fails sometimes
        // see https://discuss.gradle.org/t/getresourceasstream-returns-null-in-plugin-in-daemon-mode/2385
        new URLConnection(new URL('file:///')) {
            { setDefaultUseCaches(false) }
            void connect() throws IOException {}
        }
        this.PLUGIN_VERSION = GoogleJavaFormatPlugin.class.getResourceAsStream('/VERSION').text.trim()
    }

    private static final String EXTENSION_NAME = 'googleJavaFormat'
    private static final String DEFAULT_TASK_NAME = 'googleJavaFormat'

    private Project project
    private GoogleJavaFormatExtension extension
    private GoogleJavaFormat defaultTask

    @Override
    void apply(Project project) {
        this.project = project
        createExtension()
        createDefaultFormatTask()

        project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
            def formatTasks = graph.allTasks.findResults { Task task ->
                task instanceof GoogleJavaFormat ? (GoogleJavaFormat) task : null
            }
            configureTasks(formatTasks)
        }
    }

    private void createExtension() {
        // TODO (sherter): remove cast when future groovy versions correctly infer type
        this.extension = (GoogleJavaFormatExtension) this.project.extensions.create(EXTENSION_NAME, GoogleJavaFormatExtension)
    }


    private void createDefaultFormatTask() {
        this.defaultTask = this.project.tasks.create(DEFAULT_TASK_NAME, GoogleJavaFormat)
    }


    private void configureTasks(Collection<GoogleJavaFormat> formatTasks) {
        if (formatTasks.size() == 0) {
            return
        }
        def fileStateHandler = createFileStateHandler()
        fileStateHandler.load()
        project.gradle.buildFinished {
            fileStateHandler.flush()
        }
        for (def task : formatTasks) {
            task.setFileStateHandler(fileStateHandler)
            task.exclude { FileTreeElement f -> fileStateHandler.isUpToDate(f.file) }
            if (task.name == DEFAULT_TASK_NAME) {
                for (def sourceSet : javaSourceSets()) {
                    task.source(sourceSet)
                }
            }
        }
    }

    private FileStateHandler createFileStateHandler() {
        String buildCacheSubDir = "google-java-format/$PLUGIN_VERSION"
        return new FileStateHandler(
                this.project.projectDir,
                new File(this.project.buildDir, buildCacheSubDir),
                this.extension.toolVersion)
    }

    @TypeChecked(SKIP)
    private Collection<SourceDirectorySet> javaSourceSets() {
        if (!this.project.hasProperty('sourceSets')) {
            return []
        }
        return this.project.sourceSets.collect { SourceSet sourceSet ->
            sourceSet.java
        }
    }
}
