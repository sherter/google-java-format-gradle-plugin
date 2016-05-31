package com.github.sherter.googlejavaformatgradleplugin.test

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

class FileWithState {

    private FileTime lastModified

    final Path file
    final byte[] initialContent

    FileWithState(Path file, byte[] initialContent) {
        this.file = file
        this.initialContent = initialContent
    }

    void create() {
        Files.createDirectories(file.parent)
        Files.createFile(file)
        Files.write(file, initialContent)
        lastModified = Files.getLastModifiedTime(file)
    }

    boolean wasModified() {
        return !Files.getLastModifiedTime(file).equals(lastModified)
    }

    byte[] content() {
        return Files.readAllBytes(file)
    }

    void write(byte[] newContent) {
        Files.write(file, newContent)
    }

    void write(String newContent) {
        write(newContent.getBytes(StandardCharsets.UTF_8))
    }
}
