package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.PackageScope
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class GoogleJavaFormatTask extends SourceTask {

    /**
     * The same instance will be injected into all tasks of this type
     * (see {@link GoogleJavaFormatPlugin#createAndInjectFileStateHandler}).
     */
    private FileStateHandler fileStateHandler

    @PackageScope void setFileStateHandler(FileStateHandler fsh) {
        this.fileStateHandler = fsh
    }

    @PackageScope FileStateHandler getFileStateHandler() {
        return this.fileStateHandler
    }

    @TaskAction
    void formatSources() {
        def formatter = constructFormatter()
        source.each { file ->
            fileStateHandler.formatIfNotUpToDate(file) { fileToFormat ->
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
