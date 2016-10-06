package com.github.sherter.googlejavaformatgradleplugin;

import com.github.sherter.googlejavaformatgradleplugin.format.Formatter;
import com.github.sherter.googlejavaformatgradleplugin.format.FormatterException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;

class VerifyFileCallable implements Callable<FileInfo> {

  private final Formatter formatter;
  private final Path file;

  VerifyFileCallable(Formatter formatter, Path file) {
    this.file = Objects.requireNonNull(file);
    this.formatter = Objects.requireNonNull(formatter);
  }

  @Override
  public FileInfo call() throws PathException {
    try {
      byte[] content = Files.readAllBytes(file);
      String utf8Decoded = new String(content, StandardCharsets.UTF_8.name());
      String formatted;
      FileState state;
      try {
        formatted = formatter.format(utf8Decoded);
        state = formatted.equals(utf8Decoded) ? FileState.FORMATTED : FileState.UNFORMATTED;
      } catch (FormatterException e) {
        state = FileState.INVALID;
      }
      return FileInfo.create(file, Files.getLastModifiedTime(file), content.length, state);
    } catch (Throwable t) {
      throw new PathException(file, t);
    }
  }
}
