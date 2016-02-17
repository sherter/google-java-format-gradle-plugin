package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.PackageScope
import groovy.transform.TypeChecked
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@TypeChecked
class GoogleJavaFormat extends SourceTask {

    private static final int MAX_THREADS = 20;
    /**
     * The same instance will be injected into all tasks of this type
     * (see {@link GoogleJavaFormatPlugin#createAndInjectFileStateHandler}).
     */
    private FileStateHandler fileStateHandler
    private FormatterFactory formatterFactory

    @PackageScope void setFileStateHandler(FileStateHandler fsh) {
        this.fileStateHandler = fsh
    }

    @PackageScope FileStateHandler getFileStateHandler() {
        return this.fileStateHandler
    }

    void setFormatterFactory(FormatterFactory factory) {
        formatterFactory = factory
    }

    FormatterFactory getFormatterFactory() {
        return formatterFactory
    }

    @TaskAction
    void formatSources() {
        Formatter formatter = formatterFactory.create()
        Set<File> sourceFiles = getSource().getFiles()
        int numThreads = Math.min(sourceFiles.size(), MAX_THREADS)
        Executor executor = Executors.newFixedThreadPool(numThreads)

        sourceFiles.each { file ->
            executor.execute {
                try {
                    fileStateHandler.formatIfNotUpToDate(file) {
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
