package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.TypeChecked
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@TypeChecked
class GoogleJavaFormat extends SourceTask {

    private static final int MAX_THREADS = 20;

    private FileStateHandler fileStateHandler

    void setFileStateHandler(FileStateHandler fsh) {
        this.fileStateHandler = fsh
    }

    FileStateHandler getFileStateHandler() {
        return this.fileStateHandler
    }

    @TaskAction
    void formatSources() {
        String toolVersion = project.extensions.getByType(GoogleJavaFormatExtension).toolVersion
        Formatter formatter = new FormatterFactory(project, logger).create(toolVersion)
        Set<File> sourceFiles = getSource().getFiles()
        int numThreads = Math.min(sourceFiles.size(), MAX_THREADS)
        Executor executor = Executors.newFixedThreadPool(numThreads)

        sourceFiles.each { file ->
            executor.execute {
                try {
                    fileStateHandler.updateIfNotUpToDateAfter(file) {
                        file.write(formatter.format(file.text))
                    }
                } catch (FormatterException e) {
                    logger.error('{} is not a valid Java source file', file)
                    e.errors.each {
                        logger.info('{}:{}', file, it)
                    }
                }
            }
        }
        executor.shutdown()

        // blocks forever, unit is ignored if timeout is MAX_VALUE
        // (see http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/package-summary.html)
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
    }
}
