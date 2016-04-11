package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths
import java.nio.file.attribute.FileTime

import static com.github.sherter.googlejavaformatgradleplugin.FileInfo.create
import static com.github.sherter.googlejavaformatgradleplugin.FileState.UNFORMATTED

class FileInfoEncoderTest extends Specification {

    @Shared encoder = new FileInfoEncoder(Paths.get(''))

    def 'serialized form is as expected'() {
        expect:
        encoder.encode(info) == serialized

        where:
        info | serialized
        create(Paths.get('foo'), FileTime.fromMillis(0), 0, UNFORMATTED) | 'foo,0,0,UNFORMATTED'
        create(Paths.get('/foo'), FileTime.fromMillis(0), 0, UNFORMATTED) |
                (1..encoder.basePath.nameCount).inject('') {result, i -> result + '../'} +
                'foo,0,0,UNFORMATTED'
    }
}
