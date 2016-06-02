package com.github.sherter.googlejavaformatgradleplugin.test

import com.google.common.jimfs.Jimfs
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

class FileWithStateTest extends Specification {

    FileSystem fs = Jimfs.newFileSystem()
    Path foo = fs.getPath('foo').toAbsolutePath()

    def 'create file with missing parent directories'() {
        given:
        def file = fs.getPath('foo', 'bar', 'baz')

        when:
        FileWithState.create(file)

        then:
        Files.exists(file)
        Files.readAllBytes(file) == [] as byte[]
    }

    def 'state checks after creation'() {
        def file = FileWithState.create(foo)

        expect:
        !file.contentHasChanged()
        !file.lastModifiedTimeHasChanged()
    }

    def 'state checks after write'() {
        def file = FileWithState.create(foo)

        when:
        file.write('blub')

        then:
        !file.contentHasChanged()
        !file.lastModifiedTimeHasChanged()
    }

    def 'state checks after modification from outside'() {
        def file = FileWithState.create(foo)

        when:
        Files.setLastModifiedTime(foo, FileTime.fromMillis(0))

        then:
        file.lastModifiedTimeHasChanged()
        !file.contentHasChanged()

        when:
        Files.write(foo, 'new content'.getBytes(StandardCharsets.UTF_8))

        then:
        file.contentHasChanged()
        file.lastModifiedTimeHasChanged()
    }
}
