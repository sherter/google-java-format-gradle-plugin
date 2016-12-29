package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import com.google.common.collect.Iterables
import groovy.transform.CompileStatic
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction

import java.nio.file.Path
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@CompileStatic
class GoogleJavaFormat extends FormatTask {

  private static final Logger logger = Logging.getLogger(GoogleJavaFormat.class)

  @Override
  void accept(TaskConfigurator configurator) {
    configurator.configure(this)
    for (Path file : formattedSources) {
      logger.info("{}: UP-TO-DATE", file)
    }
  }

  List<Path> formattedSources
  Iterable<Path> filteredSources
  List<Path> invalidSources

  @TaskAction
  void formatSources() {
    boolean successful = true
    if (!Iterables.isEmpty(filteredSources)) {
      Formatter formatter = sharedContext.formatter()
      FileToStateMapper mapper = sharedContext.mapper()
      ExecutorService executor = sharedContext.executor()

      List<Future<FileInfo>> futureResults = new ArrayList<Future<FileInfo>>()
      for (Path file : filteredSources) {
        futureResults.add(executor.submit(new FormatFileCallable(formatter, file)))
      }

      for (Future<FileInfo> futureResult : futureResults) {
        try {
          FileInfo info = futureResult.get();
          mapper.putIfNewer(info);
          if (info.state() == FileState.INVALID) {
            invalidSources.add(info.path())
          }
        } catch (ExecutionException e) {
          def pathException = e.getCause() as PathException
          logger.error('{}: failed to process file', pathException.path(), pathException.getCause())
          successful = false
        }
      }
    }
    if (Iterables.size(invalidSources) > 0) {
      successful = false
      logger.error('\n\nDetected Java syntax errors in the following files ({} "{}" {}):\n',
              'you can exclude them from this task, see',
              'https://github.com/sherter/google-java-format-gradle-plugin',
              'for details')
      for (Path file : invalidSources) {
        logger.error(file.toString())
      }
    }
    if (!successful) {
      throw new GradleException("Not all files were formatted.")
    }
  }
}
