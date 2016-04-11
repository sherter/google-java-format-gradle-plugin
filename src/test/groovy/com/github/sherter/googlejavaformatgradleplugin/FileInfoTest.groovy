package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Specification

import java.nio.file.Paths
import java.nio.file.attribute.FileTime

class FileInfoTest extends Specification {

    def 'create file info with unknown state fails'() {
        when:
        FileInfo.create(Paths.get(''), FileTime.fromMillis(0), 0, FileState.UNKNOWN)

        then:
        thrown(IllegalArgumentException)
    }

    def 'path is absolute and normalized'() {
        expect:
        def info = FileInfo.create(path, FileTime.fromMillis(0L), 0, FileState.INVALID)
        info.path().isAbsolute()
        info.path().equals(info.path().normalize())

        where:
        path << [ Paths.get('foo/bar'),
                  Paths.get('foo/../bar'),
                  Paths.get('foo/bar').toAbsolutePath(),
                  Paths.get('../foo/bar').toAbsolutePath()]
    }

    def 'newer than other result'() {
        given:
        def oldResult = FileInfo.create(Paths.get(''), FileTime.fromMillis(0L), 10L, FileState.FORMATTED)
        def newResult = FileInfo.create(Paths.get(''), FileTime.fromMillis(10L), 5L, FileState.INVALID)
        def unrelated = FileInfo.create(Paths.get('foo'), FileTime.fromMillis(0L), 0L, FileState.UNFORMATTED)

        expect:
        !oldResult.isMoreRecentThan(newResult) && newResult.isMoreRecentThan(oldResult) // symmetric
        !oldResult.isMoreRecentThan(oldResult) // not reflexive

        when:
        oldResult.isMoreRecentThan(unrelated)

        then:
        thrown(IllegalArgumentException)
    }

    def 'decoded result equals original result'() {
        given:
        def encoder = new FileInfoEncoder(Paths.get('/projectDir'))
        def decoder = new FileInfoDecoder(Paths.get('/projectDir'))

        expect:
        println result
        decoder.decode(encoder.encode(result)).equals(result)

        where:
        result << [FileInfo.create(Paths.get('/foo/bar/baz'), FileTime.fromMillis(0L), 0L, FileState.INVALID),
                   FileInfo.create(Paths.get('/projectDir/foo/bar/baz'), FileTime.fromMillis(123098L), 928374L, FileState.UNFORMATTED),
                   FileInfo.create(Paths.get('/../fo,o/../b%a,r/b\\az'), FileTime.fromMillis(12309L), 986L, FileState.FORMATTED) ]
    }
}
