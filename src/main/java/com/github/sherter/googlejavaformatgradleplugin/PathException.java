package com.github.sherter.googlejavaformatgradleplugin;

import java.nio.file.Path;

class PathException extends Exception {

  private final Path path;

  PathException(Path path, Throwable cause) {
    super(cause);
    this.path = path;
  }

  Path path() {
    return path;
  }
}
