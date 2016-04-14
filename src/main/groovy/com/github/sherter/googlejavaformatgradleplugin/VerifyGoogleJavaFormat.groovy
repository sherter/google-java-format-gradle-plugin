package com.github.sherter.googlejavaformatgradleplugin

import com.google.common.collect.ImmutableList
import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask

import java.nio.file.Path
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class VerifyGoogleJavaFormat extends SourceTask implements VerificationTask, ConfigurableTask {

    boolean ignoreFailures = false
    SharedContext sharedContext
    List<Path> formattedFiles
    List<Path> unformattedFiles
    List<Path> invalidFiles
    ImmutableList<Path> filteredSources
    int fileSystemFailures = 0

    @Override
    void configure(SharedContext context) {
        this.sharedContext = context
        def sources = getSource().getFiles()
        if (sources.size() == 0) {
            return // task will be skipped (@SkipWhenEmpty in SourceTask)
        }
        def mapping = context.mapper().reverseMap(Utils.toPaths(sources))
        formattedFiles = new ArrayList<>(mapping.get(FileState.FORMATTED))
        unformattedFiles = new ArrayList<>(mapping.get(FileState.UNFORMATTED))
        invalidFiles = new ArrayList<>(mapping.get(FileState.INVALID))
        filteredSources = mapping.get(FileState.UNKNOWN)
    }

    @TaskAction
    void verifySources() {
        if (filteredSources.size() > 0) {
            computeStatesAndAddToLists()
        }

        logger.info('Properly formatted files:\n{}', formattedFiles)
        invalidFiles.each { Path file ->
            logger.lifecycle('{}: verification failed, contains syntax errors', file)
        }
        unformattedFiles.each { Path file ->
            logger.lifecycle('{}: not formatted properly', file)
        }
        if (unformattedFiles.size() + invalidFiles.size() > 0 && !ignoreFailures) {
            throw new GradleException("verification failed")
        }
    }

    private void computeStatesAndAddToLists() {
        Formatter formatter = sharedContext.formatter()
        FileToStateMapper mapper = sharedContext.mapper()
        ExecutorService executor = sharedContext.executor()

        filteredSources.collect { Path file ->
            executor.submit(new VerifyFileCallable(formatter, file))
        }.each { Future<FileInfo> futureInfo ->
            try {
                def info = futureInfo.get()
                mapper.putIfNewer(info)
                if (info.state() == FileState.FORMATTED) {
                    formattedFiles.add(info.path())
                } else if (info.state() == FileState.UNFORMATTED) {
                    unformattedFiles.add(info.path())
                } else if (info.state() == FileState.INVALID) {
                    invalidFiles.add(info.path())
                } else {
                    throw new AssertionError("no other states possible")
                }
            } catch (ExecutionException e) {
                logger.error("Error occurred while accessing file", e);
                fileSystemFailures++
            } catch (InterruptedException e) {
                throw new AssertionError("Gradle shouldn't interrupt us!", e);
            }
        }
    }
}
