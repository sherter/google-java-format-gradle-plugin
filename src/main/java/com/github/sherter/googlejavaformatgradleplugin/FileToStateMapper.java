package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/** Map files to their {@link FileState}. Designed for concurrent use in multiple threads. */
class FileToStateMapper implements Iterable<FileInfo> {

  private static final Logger log = Logging.getLogger(FileToStateMapper.class);

  private final ConcurrentHashMap<Path, FileInfo> infoCache = new ConcurrentHashMap<>();
  private final Object replaceRemoveLock = new Object();

  private final Function<Path, FileState> mapFunction =
      new Function<Path, FileState>() {
        @Override
        public FileState apply(Path path) {
          return map(path);
        }
      };

  /**
   * Associate {@code files} with their {@link FileState}.
   *
   * @see #map(Path)
   */
  ImmutableListMultimap<FileState, Path> reverseMap(Iterable<Path> files) {
    return Multimaps.index(files, mapFunction);
  }

  /**
   * Checks if we have information about the given {@code file} and returns the associated {@link
   * FileState} if this information is still valid. Otherwise returns {@link FileState#UNKNOWN}.
   *
   * <p>The information is valid if {@link FileInfo#lastModified()} and {@link FileInfo#size()}
   * equals the {@code file}'s current size and last modified time. This method always tries to
   * access the file system to verify this. Returns {@link FileState#UNKNOWN} if accessing the file
   * system fails.
   */
  FileState map(Path file) {
    file = file.toAbsolutePath().normalize();
    FileInfo info = infoCache.get(file);
    if (info == null) {
      return FileState.UNKNOWN;
    }
    FileTime currentLastModified;
    long currentSize;
    try {
      currentLastModified = Files.getLastModifiedTime(file);
      currentSize = Files.size(file);
    } catch (IOException e) {
      return FileState.UNKNOWN;
    }
    if (info.lastModified().equals(currentLastModified) && info.size() == currentSize) {
      return info.state();
    }
    log.debug("change detected; invalidating cached state for file '{}'", file);
    log.debug("timestamps (old - new): {} {}", info.lastModified(), currentLastModified);
    log.debug("sizes (old - new): {} {}", info.size(), currentSize);
    synchronized (replaceRemoveLock) {
      infoCache.remove(file);
    }
    return FileState.UNKNOWN;
  }

  /** Return the cached information (probably out-of-date) about a path or null if not in cache. */
  FileInfo get(Path path) {
    return infoCache.get(path.toAbsolutePath().normalize());
  }

  /**
   * Returns the {@code FileInfo} that is currently associated with the given {@code fileInfo}'s
   * path. If the given {@code fileInfo} is more recent than the currently associated one (according
   * to {@link FileInfo#isMoreRecentThan(FileInfo)}) than the old one is replaced by the given
   * {@code fileInfo}
   */
  FileInfo putIfNewer(FileInfo fileInfo) {
    // handle the case where no info was previously associated with the file
    FileInfo existingResult = infoCache.putIfAbsent(fileInfo.path(), fileInfo);
    if (existingResult == null) {
      return null;
    }
    // we already have information about this file, extra locking required!
    synchronized (replaceRemoveLock) {
      // get info for this file again, because it could have been
      // replaced by other threads while we were waiting for the lock,
      // i.e the (at this point non-null) value returned by putIfAbsent
      // is at this point not necessarily the latest info associated with the file
      existingResult = infoCache.get(fileInfo.path());
      if (existingResult == null || fileInfo.isMoreRecentThan(existingResult)) {
        infoCache.put(fileInfo.path(), fileInfo);
      }
      return existingResult;
    }
  }

  /**
   * Returns a "weakly consistent", unmodifiable iterator that will never throw {@link
   * ConcurrentModificationException}, and guarantees to traverse elements as they existed upon
   * construction of the iterator, and may (but is not guaranteed to) reflect any modifications
   * subsequent to construction.
   */
  @Override
  public Iterator<FileInfo> iterator() {
    return Iterators.unmodifiableIterator(infoCache.values().iterator());
  }
}
