package com.github.sherter.googlejavaformatgradleplugin

import com.google.common.base.Joiner
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask

import java.nio.file.Path
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

import static com.github.sherter.googlejavaformatgradleplugin.FileState.*
import static com.github.sherter.googlejavaformatgradleplugin.Utils.toPaths

@CompileStatic
class VerifyGoogleJavaFormat extends SourceTask implements VerificationTask, ConfigurableTask {

    boolean ignoreFailures = false

    SharedContext sharedContext


    @Override
    void configure(SharedContext context) {
        this.sharedContext = context
        List<Object> ownSources = super.@source
        if (ownSources.isEmpty()) {
            setSource(context.extension.getSource())
        }
    }

    @TaskAction
    void verifySources() {
        def mapping = sharedContext.mapper().reverseMap(toPaths(getSource().files))

        def formatted = new ArrayList<Path>(mapping.get(FORMATTED))
        def unformatted = new ArrayList<Path>(mapping.get(UNFORMATTED))
        def invalid = new ArrayList<Path>(mapping.get(INVALID))

        def successful = processUnknown(mapping.get(UNKNOWN), formatted, unformatted, invalid)
        for(Path p : formatted) {
            logger.info('{}: verified proper formatting', p)
        }
        if (unformatted.size() > 0) {
            logger.lifecycle('\n\nThe following files are not formatted properly:\n')
            for(Path file : unformatted) {
                logger.lifecycle(file.toString())
            }
        }
        if (invalid.size() > 0) {
            logger.lifecycle('\n\nDetected Java syntax errors in the following files ({} "{}" {}):\n',
                    'you can configure this task to exclude them, see',
                    'https://github.com/sherter/google-java-format-gradle-plugin',
                    'for details')
            for (Path file : invalid) {
                logger.lifecycle(file.toString())
            }
        }

        def problems = [] as List<String>
        if (!successful) {
            problems.add('I/O errors')
        }
        if (invalid.size() > 0) {
            problems.add('syntax errors')
        }
        if (unformatted.size() > 0) {
            problems.add('formatting style violations')
        }
        if (problems.size() > 0) {
            def message = 'Problems: ' + Joiner.on(', ').join(problems)
            if (ignoreFailures) {
                logger.warn(message)
            } else {
                throw new GradleException(message)
            }
        }
    }

    private boolean processUnknown(List<Path> unknown, List<Path> formatted, List<Path> unformatted, List<Path> invalid) {
        boolean successful = true
        if (unknown.size() > 0) {
            def mapper = sharedContext.mapper()
            def formatter = sharedContext.formatter()
            def executor = sharedContext.executor()

            def futures = unknown.collect { Path file ->
                executor.submit(new VerifyFileCallable(formatter, file))
            }
            for (Future<FileInfo> futureInfo : futures) {
                try {
                    def info = futureInfo.get()
                    mapper.putIfNewer(info)
                    if (info.state() == FORMATTED) {
                        formatted.add(info.path())
                    } else if (info.state() == UNFORMATTED) {
                        unformatted.add(info.path())
                    } else if (info.state() == INVALID) {
                        invalid.add(info.path())
                    } else {
                        //throw new AssertionError('no other states possible')
                    }
                } catch (ExecutionException e) {
                    def pathException = e.getCause() as PathException
                    logger.error('{}: failed to process file', pathException.path(), pathException.getCause())
                    successful = false
                }
            }
        }
        return successful
    }
}
