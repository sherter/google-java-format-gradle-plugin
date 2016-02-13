package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class GoogleJavaFormatTask extends SourceTask {

    FileStateHandler fileStateHandler

    @TaskAction
    void formatSources() {
        def formatter = constructFormatter()
        source.each { file ->
            this.fileStateHandler.formatIfNotUpToDate(file) { fileToFormat ->
                fileToFormat.write(formatter.formatSource(fileToFormat.text))
            }
        }
    }

    private constructFormatter() {
        def urls = project.configurations[GoogleJavaFormatPlugin.CONFIGURATION_NAME].files.collect {
            it.toURI().toURL()
        }
        def classLoader = new URLClassLoader(urls as URL[], ClassLoader.getSystemClassLoader())
        return classLoader.loadClass('com.google.googlejavaformat.java.Formatter').newInstance()
    }
}
