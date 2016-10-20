package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileTree

@CompileStatic
class GoogleJavaFormatPlugin implements Plugin<Project> {

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

    @Override
    void apply(Project project) {
        this.project = project
        createExtension()
        createDefaultTasks()

        SharedContext context = new SharedContext(project, extension)
        project.gradle.taskGraph.beforeTask { Task task ->
            if (task instanceof ConfigurableTask) {
                ((ConfigurableTask) task).configure(context)
            }
        }
    }

    private void createExtension() {
        extension = project.extensions.create(EXTENSION_NAME, GoogleJavaFormatExtension, project)
    }


    private void createDefaultTasks() {
        this.project.tasks.create(DEFAULT_FORMAT_TASK_NAME, GoogleJavaFormat)
        def defaultVerifyTask = this.project.tasks.create(DEFAULT_VERIFY_TASK_NAME, VerifyGoogleJavaFormat)
        makeCheckTaskDependOn(defaultVerifyTask)
    }

    private void makeCheckTaskDependOn(Task task) {
        def checkTask = project.tasks.findByName('check')
        if (checkTask != null) {
            checkTask.dependsOn(task)
        } else {
            project.tasks.whenTaskAdded { Task t ->
                if (t.name == 'check') {
                    t.dependsOn(task)
                }
            }
        }
    }
}
