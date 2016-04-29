package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.FormatterException
import com.google.common.jimfs.Jimfs
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException

class FormatFileCallableTest extends Specification {

    def formatter = Mock(Formatter)
    def file = Jimfs.newFileSystem().getPath('Foo.java').toAbsolutePath()
    def task = new FormatFileCallable(formatter, file)

    def 'format non existing file throws'() {
        when:
        task.call()

        then:
        PathException e = thrown()
        e.getCause() instanceof NoSuchFileException
        0 * formatter.format(_)
    }

    def 'format invalid java file'() {
        given:
        def fileContent = 'Hello World!'.getBytes(StandardCharsets.UTF_8.name())
        Files.write(file, fileContent)
        def modifiedTime = Files.getLastModifiedTime(file)

        when:
        def result = task.call()

        then:
        1 * formatter.format('Hello World!') >> { throw new FormatterException() }
        result.path() == file
        result.state() == FileState.INVALID
        result.lastModified() == modifiedTime
        result.size() == fileContent.length
    }


    def 'format file that is already formatted properly'() {
        given:
        def fileContent = 'Hello World!'.getBytes(StandardCharsets.UTF_8.name())
        Files.write(file, fileContent)
        def modifiedTime = Files.getLastModifiedTime(file)

        when:
        def result = task.call()

        then:
        1 * formatter.format(_) >> { args -> args[0] }
        result.path() == file
        result.state() == FileState.FORMATTED
        result.lastModified() == modifiedTime
        result.size() == fileContent.length
    }

    def 'format and write formatted content to file'() {
        given:
        Files.write(file, 'unformatted'.getBytes(StandardCharsets.UTF_8.name()))

        when:
        def result = task.call()

        then:
        1 * formatter.format('unformatted') >> 'formatted'
        result.path() == file
        result.state() == FileState.FORMATTED
        result.lastModified() == Files.getLastModifiedTime(file)
        result.size() == Files.size(file)
        'formatted' == new String(Files.readAllBytes(file), StandardCharsets.UTF_8.name())
    }
}
