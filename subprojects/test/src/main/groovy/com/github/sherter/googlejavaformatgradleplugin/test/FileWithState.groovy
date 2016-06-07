package com.github.sherter.googlejavaformatgradleplugin.test

import groovy.transform.PackageScope

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

class FileWithState {

    private final Path file
    private byte[] lastWrittenContent
    private FileTime lastWrittenTime

    private FileWithState(Path file, byte[] content, FileTime time) {
        this.file = file
        this.lastWrittenContent = content
        this.lastWrittenTime = time
    }

    @PackageScope
    static FileWithState create(Path file) {
        Files.createDirectories(file.parent)
        Files.createFile(file)
        return new FileWithState(file, [] as byte[], Files.getLastModifiedTime(file))
    }

    /**
     * Returns true if the file's current last modified timestamp is not equal
     * to the timestamp that was recorded when {@code write} was called last (or
     * creation time, if never called).
     */
    boolean lastModifiedTimeHasChanged() {
        return !Files.getLastModifiedTime(file).equals(lastWrittenTime)
    }

    /**
     * Returns true if the file's current content is not equal to the content
     * that was recorded when {@code write} was called last (or when it's empty and
     * write was never called before).
     */
    boolean contentHasChanged() {
        return !Arrays.equals(readBytes(), lastWrittenContent)
    }


    byte[] readBytes() {
        return Files.readAllBytes(file)
    }

    /** Reads and returns file content decoded as UTF8. */
    String read() {
        return new String(readBytes(), StandardCharsets.UTF_8)
    }

    /**
     * Writes new content to the file and remembers the content and the last modified time
     * so we can detect changes from outside with {@link FileWithState#lastModifiedTimeHasChanged()}
     * and {@link FileWithState#contentHasChanged()}.
     */
    void write(byte[] newContent) {
        Files.write(file, newContent)
        lastWrittenContent = newContent
        lastWrittenTime = Files.getLastModifiedTime(file)
    }

    /**
     * Same as {@link FileWithState#write(byte[])}, encoded with UTF8.
     */
    void write(String newContent) {
        write(newContent.getBytes(StandardCharsets.UTF_8))
    }

    /**
     * Deletes the file from the file system.
     */
    void delete() {
        Files.delete(file)
    }
}
