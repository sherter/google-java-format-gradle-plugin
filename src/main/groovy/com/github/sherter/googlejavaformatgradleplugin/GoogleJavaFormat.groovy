package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

class GoogleJavaFormat extends SourceTask implements ConfigurableTask {

  private SharedContext sharedContext;
  private Iterable<Path> filteredSources;

  @Override
  public void configure(SharedContext context) {
    this.sharedContext = context;
    Set<File> sources = getSource().getFiles();
    if (sources.size() == 0) {
      return; // task will be skipped (@SkipWhenEmpty in SourceTask)
    }
    ListMultimap<FileState, Path> mapping = context.mapper().reverseMap(Utils.toPaths(sources));
    List<Path> formattedFiles = mapping.get(FileState.FORMATTED);
    if (formattedFiles.size() > 0) {
      getLogger().info("Skipping formatted files:\n{}", formattedFiles);
    }
    List<Path> invalidFiles = mapping.get(FileState.INVALID);
    if (invalidFiles.size() > 0) {
      getLogger().info("Skipping invalid Java files:\n{}", invalidFiles);
    }
    List<Path> unformattedFiles = mapping.get(FileState.UNFORMATTED);
    List<Path> unknownFiles = mapping.get(FileState.UNKNOWN);
    if (unformattedFiles.size() + unknownFiles.size() == 0) {
      // task is up-to-date, make it skip
      setSource(Collections.emptyList());
    } else {
      filteredSources = Iterables.concat(unformattedFiles, unknownFiles);
    }
  }

  @TaskAction
  void formatSources() {
    Formatter formatter = sharedContext.formatter();
    FileToStateMapper mapper = sharedContext.mapper();
    ExecutorService executor = sharedContext.executor();

    List<Future<FileInfo>> futureResults = new ArrayList<>();
    for (Path file : filteredSources) {
      futureResults.add(executor.submit(new FormatFileCallable(formatter, file)));
    }

    for (Future<FileInfo> futureResult : futureResults) {
      try {
        FileInfo info = futureResult.get();
        mapper.putIfNewer(info);
        if (info.state() == FileState.INVALID) {
          getLogger().error("{}: found syntax errors, skipping", info.path());
        } else if (info.state() == FileState.FORMATTED) {
          getLogger().info("{}: successfully formatted", info.path());
        } else {
          throw new AssertionError("no other states possible");
        }
      } catch (ExecutionException e) {
        getLogger().error("Error occurred while trying to format file", e);
      } catch (InterruptedException e) {
        throw new AssertionError("Gradle shouldn't interrupt us!", e);
      }
    }
  }
}
