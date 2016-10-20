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
class GoogleJavaFormat extends SourceTask implements ConfigurableTask {

  private static final Logger logger = Logging.getLogger(GoogleJavaFormat.class)

  private SharedContext sharedContext
  private Iterable<Path> filteredSources
  private List<Path> invalidSources

  @Override
  public void configure(SharedContext context) {
    this.sharedContext = context
    List<Object> ownSources = super.@source
    if (ownSources.isEmpty()) {
      setSource(context.extension.getSource())
    }
    def mapping = context.mapper().reverseMap(Utils.toPaths(getSource().files))
    def formattedFiles = mapping.get(FileState.FORMATTED)
    for (Path file : formattedFiles) {
      logger.info("{}: UP-TO-DATE", file)
    }
    invalidSources = new ArrayList<>(mapping.get(FileState.INVALID))
    def unformatted = mapping.get(FileState.UNFORMATTED)
    def unknown = mapping.get(FileState.UNKNOWN);
    if (Iterables.size(invalidSources) + Iterables.size(unformatted) + Iterables.size(unknown) == 0) {
      // task is up-to-date, make sure it is skipped
      setSource(Collections.emptyList())
    } else {
      filteredSources = Iterables.concat(unformatted, unknown);
    }
  }

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
