package com.github.sherter.googlejavaformatgradleplugin

import com.google.common.jimfs.Jimfs
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException

class VerifyFileCallableTest extends Specification {

    def formatter = Mock(Formatter)
    def file = Jimfs.newFileSystem().getPath('Foo.java').toAbsolutePath()
    def task = new VerifyFileCallable(formatter, file)

    def 'verify non existing file throws'() {
        when:
        task.call()

        then:
        NoSuchFileException e = thrown()
        0 * formatter.format(_)
    }

    def 'verify invalid java file'() {
        given:
        Files.write(file, 'foo'.getBytes(StandardCharsets.UTF_8.name()))
        def modified = Files.getLastModifiedTime(file)

        when:
        def result = task.call()

        then:
        1 * formatter.format('foo') >> { throw FormatterException.create([]) }
        result.state() == FileState.INVALID
        result.lastModified() == modified
        result.size() == Files.size(file)
        result.path() == file
    }

    def 'verify unformatted java file'() {
        given:
        Files.write(file, 'foo'.getBytes(StandardCharsets.UTF_8.name()))
        def modified = Files.getLastModifiedTime(file)

        when:
        def result = task.call()

        then:
        1 * formatter.format('foo') >> 'bar'
        result.state() == FileState.UNFORMATTED
        result.lastModified() == modified
        result.size() == Files.size(file)
        result.path() == file
    }

    def 'verify correctly formatted java file'() {
        given:
        Files.write(file, 'foo'.getBytes(StandardCharsets.UTF_8.name()))
        def modified = Files.getLastModifiedTime(file)

        when:
        def result = task.call()

        then:
        1 * formatter.format('foo') >> 'foo'
        result.state() == FileState.FORMATTED
        result.lastModified() == modified
        result.size() == Files.size(file)
        result.path() == file
    }
}
