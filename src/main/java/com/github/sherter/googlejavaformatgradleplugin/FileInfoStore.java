package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import org.gradle.api.logging.Logger;

/**
 * Persistent fileInfoStore for {@link FileInfo} objects.
 *
 * <p>It's not safe to use instances of this class concurrently in multiple threads.
 */
class FileInfoStore {

  private final Logger log;
  private final Path path;
  private final FileInfoDecoder decoder;
  private final FileInfoEncoder encoder;
  private Map<Path, FileInfo> readCache;
  private FileChannel channel;

  @Inject
  FileInfoStore(
      Logger logger,
      @Named("storage") Path path,
      FileInfoEncoder encoder,
      FileInfoDecoder decoder) {
    this.log = Objects.requireNonNull(logger);
    this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
    this.encoder = Objects.requireNonNull(encoder);
    this.decoder = Objects.requireNonNull(decoder);
  }

  /**
   * Reads serialized {@link FileInfo} objects from this {@link FileInfoStore}'s backing file and
   * returns deserialized {@link FileInfo} objects.
   *
   * <p>The method succeeds if the backing file can be accessed as required and the file's general
   * format is intact. Decoding errors for single elements are logged, but don't prevent the method
   * from succeeding.
   *
   * @throws IOException if an I/O error occurs
   */
  ImmutableSet<FileInfo> read() throws IOException {
    log.debug("Reading file states from {}", path);
    if (channel == null) {
      init();
    }
    channel.position(0);
    BufferedReader reader =
        new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8.name()));
    readCache = new HashMap<>();
    String line;
    int lineNumber = 1;
    while ((line = reader.readLine()) != null) {
      try {
        FileInfo r = decoder.decode(line);
        readCache.put(r.path(), r);
      } catch (IllegalArgumentException e) {
        log.error("{}:{}: couldn't decode '{}': {}", path, lineNumber, line, e.getMessage());
      }
      lineNumber++;
    }
    return ImmutableSet.copyOf(readCache.values());
  }

  private void init() throws IOException {
    Files.createDirectories(path.getParent());
    channel =
        FileChannel.open(
            path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
  }

  /**
   * Insert the given {@link FileInfo}'s into the persistent fileInfoStore.
   *
   * <p>If the fileInfoStore already contains information about a path that is referenced in an
   * element in {@code updates}, then this information is replaced. If {@code updates} contain
   * multiple {@link FileInfo} objects for the same path, the last one in iteration order is
   * inserted.
   *
   * @throws IOException if an I/O error occurs
   */
  void update(Iterable<FileInfo> updates) throws IOException {
    if (readCache == null) {
      read();
    }
    Map<Path, FileInfo> cacheCopy = new HashMap<>(readCache);
    for (FileInfo result : updates) {
      cacheCopy.put(result.path(), result);
    }
    log.debug("Writing updated file states to {}:\n{}", path, cacheCopy.values());
    channel.truncate(0);
    BufferedWriter writer =
        new BufferedWriter(Channels.newWriter(channel, StandardCharsets.UTF_8.name()));
    for (FileInfo result : cacheCopy.values()) {
      writer.write(encoder.encode(result));
      writer.newLine();
    }
    writer.flush();
    readCache = cacheCopy;
  }

  void clear() throws IOException {
    if (channel == null) {
      init();
    }
    channel.truncate(0);
    channel.force(false);
  }

  void close() throws IOException {
    if (channel == null) {
      return;
    }
    channel.close();
  }
}
