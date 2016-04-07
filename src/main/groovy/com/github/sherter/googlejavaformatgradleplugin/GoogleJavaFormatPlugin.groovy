package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileTree

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

    static final String EXTENSION_NAME = 'googleJavaFormat'
    static final String DEFAULT_FORMAT_TASK_NAME = 'googleJavaFormat'
    static final String DEFAULT_VERIFY_TASK_NAME = 'verifyGoogleJavaFormat'

    private Project project
    private GoogleJavaFormatExtension extension
    private FileStateHandler fileStateHandler

    @Override
    void apply(Project project) {
        this.project = project
        createExtension()
        createDefaultTasks()

        project.gradle.taskGraph.whenReady {
            if (graphContainsConfigurableTasks()) {
                setupFileStateHandler()
                configureTasksBeforeExecution()
            }
        }
    }


    private void createExtension() {
        // TODO (sherter): remove cast when future groovy versions correctly infer type
        this.extension = (GoogleJavaFormatExtension) this.project.extensions.create(EXTENSION_NAME, GoogleJavaFormatExtension)
    }


    private void createDefaultTasks() {
        FileTree defaultInputs = defaultInputs()
        this.project.tasks.create(DEFAULT_FORMAT_TASK_NAME, GoogleJavaFormat).setSource(defaultInputs)
        this.project.tasks.create(DEFAULT_VERIFY_TASK_NAME, VerifyGoogleJavaFormat).setSource(defaultInputs)
    }

    private FileTree defaultInputs() {
        ConfigurableFileTree javaFiles = project.fileTree(dir: project.projectDir, includes: ['**/*.java'])
        project.allprojects { Project p ->
            javaFiles.exclude p.buildDir.path.substring(project.projectDir.path.length() + 1)
        }
        return javaFiles
    }


    private boolean graphContainsConfigurableTasks() {
        Task foundTask = project.gradle.taskGraph.allTasks.find { Task task ->
            task instanceof ConfigurableTask
        }
        return foundTask != null
    }

    private void setupFileStateHandler() {
        String buildCacheSubDir = "google-java-format/$PLUGIN_VERSION"
        this.fileStateHandler = new FileStateHandler(
                this.project.projectDir,
                new File(this.project.buildDir, buildCacheSubDir),
                this.extension.toolVersion)
        fileStateHandler.load()
        project.gradle.buildFinished { fileStateHandler.flush() }
    }

    private void configureTasksBeforeExecution() {
        def context = new SharedContext(fileStateHandler)
        project.gradle.taskGraph.beforeTask { Task task ->
            if (task instanceof ConfigurableTask) {
                ((ConfigurableTask) task).configure(context)
            }
        }
    }
}
