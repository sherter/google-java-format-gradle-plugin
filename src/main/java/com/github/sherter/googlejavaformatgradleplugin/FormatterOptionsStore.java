package com.github.sherter.googlejavaformatgradleplugin;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

class FormatterOptionsStore {

  private final Path backingFile;
  private FileChannel channel;

  @Inject
  FormatterOptionsStore(@Named("settings") Path backingFile) {
    this.backingFile = backingFile.toAbsolutePath().normalize();
  }

  FormatterOptions read() throws IOException {
    if (channel == null) {
      init();
    }
    channel.position(0);
    Reader reader = Channels.newReader(channel, StandardCharsets.UTF_8.name());
    Properties properties = new Properties();
    properties.load(reader);
    return FormatterOptions.create(properties.getProperty("toolVersion"));
  }

  private void init() throws IOException {
    Files.createDirectories(backingFile.getParent());
    channel =
        FileChannel.open(
            backingFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE);
  }

  void write(FormatterOptions options) throws IOException {
    if (channel == null) {
      init();
    }
    channel.truncate(0);
    Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8.name());
    Properties properties = new Properties();
    properties.setProperty("toolVersion", options.version());
    properties.store(writer, "Generated; DO NOT CHANGE!!!");
  }
}
