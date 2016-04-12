package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Persistent store for {@link FileInfo} objects.
 *
 * It's not safe to use instances of this class concurrently in multiple threads.
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
   * Reads serialized {@link FileInfo} objects from this {@link FileInfoStore}'s backing file
   * and returns deserialized {@link FileInfo} objects.
   *
   * The method succeeds if the backing file can be accessed as required and the file's general format is intact.
   * Decoding errors for single elements are logged, but don't prevent the method from succeeding.
   *
   * @throws IOException if an I/O error occurs
   */
  ImmutableSet<FileInfo> read() throws IOException {
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
    channel =
        FileChannel.open(
            path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    FileLock lock = channel.tryLock();
    if (lock == null) {
      throw new IOException("File " + path + " is locked by another program");
    }
  }

  /**
   * Insert the given {@link FileInfo}'s into the persistent store.
   *
   * If the store already contains information about a path that is referenced in an element in {@code updates},
   * then this information is replaced. If {@code updates} contain multiple {@link FileInfo} objects for the
   * same path, the last one in iteration order is inserted.
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
}
