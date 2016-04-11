package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.IOError;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable and therefore thread safe.
 */
class FileInfoDecoder {
  private final Path basePath; // absolute and normalized

  /**
   * Constructs a new {@link FileInfoDecoder} that resolves serialized path strings against {@code basePath}.
   *
   * @throws IOError if {@code basePath} is not absolute and {@link Path#toAbsolutePath()} fails
   */
  FileInfoDecoder(Path basePath) {
    this.basePath = Objects.requireNonNull(basePath).toAbsolutePath().normalize();
  }

  /**
   * Deserialize the given {@code serializedFileInfo}.
   *
   * @throws IllegalArgumentException if {@code serializedFileInfo} is not a valid serialization
   * of a {@link FileInfo} object according to {@link FileInfoEncoder}
   */
  FileInfo decode(CharSequence serializedFileInfo) {
    String[] elements = Iterables.toArray(Splitter.on(',').split(serializedFileInfo), String.class);
    if (elements.length != 4) {
      throw new IllegalArgumentException("Invalid number of elements");
    }
    return FileInfo.create(
        decodePath(elements[0]),
        FileTime.fromMillis(decodeLong(elements[1])),
        decodeLong(elements[2]),
        decodeState(elements[3]));
  }

  private Path decodePath(CharSequence path) {
    Iterable<String> nameElements = Splitter.on('/').split(path);
    List<String> decodedNameElements = new ArrayList<>();
    for (String element : nameElements) {
      try {
        String decoded = URLDecoder.decode(element, StandardCharsets.UTF_8.name());
        decodedNameElements.add(decoded);
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError("Java spec requires UTF-8 to be supported");
      }
    }
    String relativePathString =
        Joiner.on(basePath.getFileSystem().getSeparator()).join(decodedNameElements);
    return basePath.resolve(relativePathString);
  }

  private FileState decodeState(String state) {
    try {
      return FileState.valueOf(state);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Not a valid state: " + state);
    }
  }

  private long decodeLong(String number) {
    try {
      return Long.valueOf(number);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Not a valid long value: " + number);
    }
  }
}
