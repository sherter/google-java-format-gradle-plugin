package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.base.Joiner;

import java.io.IOError;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable and therefore thread safe.
 */
class FileInfoEncoder {
  private final Path basePath; // absolute and normalized

  /**
   * Constructs a new {@link FileInfoEncoder} that encodes a {@link FileInfo}'s path to
   * a unix path string that is relative to {@code basePath}.
   *
   * @throws IOError if {@code basePath} is not absolute and {@link Path#toAbsolutePath()} fails
   */
  FileInfoEncoder(Path basePath) {
    this.basePath = Objects.requireNonNull(basePath).toAbsolutePath().normalize();
  }

  String encode(FileInfo fileInfo) {
    return encodePath(fileInfo.path())
        + ','
        + fileInfo.lastModified().toMillis()
        + ','
        + fileInfo.size()
        + ','
        + fileInfo.state().name();
  }

  private String encodePath(Path p) {
    assert p.isAbsolute();
    Path relativeToBase = basePath.relativize(p);
    List<String> urlEncodedElements = new ArrayList<>();
    for (Path element : relativeToBase) {
      try {
        String encoded = URLEncoder.encode(element.toString(), StandardCharsets.UTF_8.name());
        urlEncodedElements.add(encoded);
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError("Java spec requires UTF-8 to be supported");
      }
    }
    return Joiner.on('/').join(urlEncodedElements);
  }
}
