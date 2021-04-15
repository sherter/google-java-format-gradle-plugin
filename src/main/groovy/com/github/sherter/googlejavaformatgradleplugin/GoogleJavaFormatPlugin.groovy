package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.GradleVersion

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

    static final boolean SUPPORTS_LAZY_TASKS = GradleVersion.current() >= GradleVersion.version("4.9")

    private Project project
    private GoogleJavaFormatExtension extension

    @Override
    void apply(Project project) {
        this.project = project
        createExtension()
        createDefaultTasks()

        SharedContext context = new SharedContext(project, extension)
        TaskConfigurator configurator = new TaskConfigurator(context)
        project.afterEvaluate {
            project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                graph.getAllTasks().stream().filter {
                    it instanceof FormatTask
                }.map{
                    if(it instanceof FormatTask){
                        it.accept(configurator)
                    }
                }
            }
        }
    }

    private void createExtension() {
        extension = project.extensions.create(EXTENSION_NAME, GoogleJavaFormatExtension, project)
    }


    private void createDefaultTasks() {
        def defaultVerifyTask
        if (SUPPORTS_LAZY_TASKS) {
            this.project.tasks.register(DEFAULT_FORMAT_TASK_NAME, GoogleJavaFormat)
            defaultVerifyTask = this.project.tasks.register(DEFAULT_VERIFY_TASK_NAME, VerifyGoogleJavaFormat)
        } else {
            this.project.tasks.create(DEFAULT_FORMAT_TASK_NAME, GoogleJavaFormat)
            defaultVerifyTask = this.project.tasks.create(DEFAULT_VERIFY_TASK_NAME, VerifyGoogleJavaFormat)
        }
        makeCheckTaskDependOn(defaultVerifyTask)
    }

    private void makeCheckTaskDependOn(Object task) {
        if (SUPPORTS_LAZY_TASKS) {
            project.plugins.withType(LifecycleBasePlugin) {
                project.tasks.named('check').configure {
                    it.dependsOn(task)
                }
            }
        } else {
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
}
