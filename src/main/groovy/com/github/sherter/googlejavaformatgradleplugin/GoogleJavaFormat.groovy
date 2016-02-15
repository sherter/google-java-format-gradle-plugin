package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.PackageScope
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GoogleJavaFormat extends SourceTask {

    private static final MAX_THREADS = 20;
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
        Set<File> sourceFiles = source.getFiles()
        int numThreads = Math.min(sourceFiles.size(), MAX_THREADS)
        Executor executor = Executors.newFixedThreadPool(numThreads)

        sourceFiles.each { file ->
            executor.execute {
                fileStateHandler.formatIfNotUpToDate(file) { fileToFormat ->
                    fileToFormat.write(formatter.formatSource(fileToFormat.text))
                }
            }
        }
        executor.shutdown()

        // blocks forever, unit is ignored if timeout is MAX_VALUE
        // (see http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/package-summary.html)
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
    }

    private constructFormatter() {
        def urls = project.configurations[GoogleJavaFormatPlugin.CONFIGURATION_NAME].files.collect {
            it.toURI().toURL()
        }
        def classLoader = new URLClassLoader(urls as URL[], ClassLoader.getSystemClassLoader())
        return classLoader.loadClass('com.google.googlejavaformat.java.Formatter').newInstance()
    }
}
