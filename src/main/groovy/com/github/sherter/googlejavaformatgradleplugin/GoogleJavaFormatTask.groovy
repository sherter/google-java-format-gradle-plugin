package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class GoogleJavaFormatTask extends SourceTask {

    @TaskAction
    void formatJavaSources() {
        def urls = project.configurations[GoogleJavaFormatPlugin.CONFIGURATION_NAME].files.collect {
            it.toURI().toURL()
        }
        def classLoader = new URLClassLoader(urls as URL[], ClassLoader.getSystemClassLoader())
        def formatter = classLoader.loadClass('com.google.googlejavaformat.java.Formatter').newInstance()
        // TODO(sherter): Should be run in parallel
        source.each { file ->
            file.write(formatter.formatSource(file.text))
        }
    }
}
