package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.TypeChecked
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@TypeChecked
class GoogleJavaFormat extends SourceTask implements ConfigurableTask {

    private static final int MAX_THREADS = 20;

    SharedContext context

    @Override
    void configure(SharedContext context) {
        this.context = context
        exclude { FileTreeElement f -> context.fileStateHandler().isUpToDate(f.file) }
    }

    @TaskAction
    void formatSources() {
        Formatter formatter = context.formatter()
        Set<File> sourceFiles = getSource().getFiles()
        int numThreads = Math.min(sourceFiles.size(), MAX_THREADS)
        Executor executor = Executors.newFixedThreadPool(numThreads)
        FileStateHandler fileStateHandler = context.fileStateHandler()

        sourceFiles.each { file ->
            executor.execute {
                try {
                    fileStateHandler.updateIfNotUpToDateAfter(file) {
                        def content = file.getText(StandardCharsets.UTF_8.name())
                        file.write(formatter.format(content), StandardCharsets.UTF_8.name())
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
