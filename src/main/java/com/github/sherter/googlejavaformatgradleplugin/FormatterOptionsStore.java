package com.github.sherter.googlejavaformatgradleplugin;

import com.github.sherter.googlejavaformatgradleplugin.format.Style;
import com.google.common.base.Enums;
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
import javax.inject.Inject;
import javax.inject.Named;

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
    String styleProperty = properties.getProperty("style");
    Style style = null;
    if (styleProperty != null) {
      style = Enums.getIfPresent(Style.class, styleProperty).orNull();
    }
    return FormatterOptions.create(properties.getProperty("toolVersion"), style);
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
    properties.setProperty("style", options.style().name());
    properties.store(writer, "Generated; DO NOT CHANGE!!!");
  }

  void close() throws IOException {
    if (channel == null) {
      return;
    }
    channel.close();
  }
}
