package com.github.sherter.googlejavaformatgradleplugin;

import com.google.auto.value.AutoValue;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Immutable value type that associates a file at a specific point in time with its formatting
 * state.
 */
@AutoValue
abstract class FileInfo {

  /**
   * Constructs a new object of type {@link FileInfo} storing the given values.
   *
   * <p>If {@code path} is not already absolute and normalized, an absolute and normalized path that
   * is equivalent to {@code path} is stored instead.
   *
   * @throws IllegalArgumentException if {@code state} == {@link FileState#UNKNOWN}
   */
  static FileInfo create(Path path, FileTime lastModified, long size, FileState state) {
    if (state == FileState.UNKNOWN) {
      throw new IllegalArgumentException(
          "constructing file info with state UNKNOWN is not allowed");
    }
    Path modifiedPath = path.toAbsolutePath().normalize();
    return new AutoValue_FileInfo(modifiedPath, lastModified, size, state);
  }

  /**
   * Returns the path of the file this object contains information about. The returned path is
   * {@link Path#isAbsolute() absolute} and {@link Path#normalize() normalized}.
   */
  abstract Path path();

  abstract FileTime lastModified();

  abstract long size();

  abstract FileState state();

  /**
   * Returns true if and only if {@code this} FileInfo represents the file at a strictly later point
   * in time than the given FileInfo.
   *
   * @throws IllegalArgumentException if {@code other} represents a different file
   */
  boolean isMoreRecentThan(FileInfo other) {
    if (!path().equals(other.path())) {
      throw new IllegalArgumentException("Can't compare info for different files");
    }
    return lastModified().compareTo(other.lastModified()) > 0;
  }
}
