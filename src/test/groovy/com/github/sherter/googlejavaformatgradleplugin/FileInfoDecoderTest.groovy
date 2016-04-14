package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Specification

import java.nio.file.Paths
import java.nio.file.attribute.FileTime

import static com.github.sherter.googlejavaformatgradleplugin.FileInfo.create
import static com.github.sherter.googlejavaformatgradleplugin.FileState.*

class FileInfoDecoderTest extends Specification {

    def 'decode valid'() {
        given:
        def decoder = new FileInfoDecoder(Paths.get(''))

        expect:
        decoder.decode(encoded) equals decoded

        where:
        encoded | decoded
        'foo,13000000,10,UNFORMATTED' | create(Paths.get('foo'), FileTime.fromMillis(13), 10, UNFORMATTED)
        'b%2Cr,-231000000,0,INVALID' | create(Paths.get('b,r'), FileTime.fromMillis(-231), 0, INVALID)
        '../foo/bar,0,0,FORMATTED' | create(Paths.get('../foo/bar'), FileTime.fromMillis(0), 0, FORMATTED)
    }

    def 'decode invalid'() {
        given:
        def decoder = new FileInfoDecoder(Paths.get(''))

        when:
        decoder.decode(encoded)

        then:
        IllegalArgumentException e = thrown()
        e.message == errorMessage

        where:
        encoded | errorMessage
        'b,r,-231,0,UNKNOWN' | 'Invalid number of elements'
        'b%r,231,0,UNKNOWN' | 'URLDecoder: Incomplete trailing escape (%) pattern'
        'foo,0,0,STATE' | 'Not a valid state: STATE'
        'foo,abc,0,STATE' | 'Not a valid long value: abc'
    }
}
