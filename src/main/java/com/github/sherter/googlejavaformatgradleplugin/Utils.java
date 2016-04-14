package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import java.io.File;
import java.nio.file.Path;

class Utils {

  private static final Function<File, Path> toPath =
      new Function<File, Path>() {
        @Override
        public Path apply(File file) {
          return file.toPath();
        }
      };

  private static final Function<Path, File> toFiles =
      new Function<Path, File>() {
        @Override
        public File apply(Path path) {
          return path.toFile();
        }
      };

  static Iterable<Path> toPaths(Iterable<File> files) {
    return Iterables.transform(files, toPath);
  }

  static Iterable<File> toFiles(Iterable<Path> paths) {
    return Iterables.transform(paths, toFiles);
  }
}
