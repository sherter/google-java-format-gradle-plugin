package com.github.sherter.googlejavaformatgradleplugin

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.gradle.api.logging.Logger
import org.slf4j.helpers.MessageFormatter
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

class FileInfoStoreTest extends Specification {

    Logger log = Mock()
    FileSystem fs = Jimfs.newFileSystem(
            Configuration.unix().toBuilder().setWorkingDirectory('/base').build())
    def encoder = new FileInfoEncoder(fs.getPath(''))
    def decoder = new FileInfoDecoder(fs.getPath(''))
    def backingFile = fs.getPath('storage.txt')

    def 'read returns expected result and decoding errors are logged'() {
        given:
        def store = new FileInfoStore(log, backingFile, encoder, decoder)
        Files.write(backingFile,
                '''foo,0,0,FORMATTED
                  |this-one-errors
                  |baz,0,0,INVALID
                  |'''.stripMargin().getBytes(StandardCharsets.UTF_8))

        when:
        def readResult = store.read()

        then:
        readResult.equals([ FileInfo.create(fs.getPath('foo'), FileTime.fromMillis(0), 0, FileState.FORMATTED),
                            FileInfo.create(fs.getPath('baz'), FileTime.fromMillis(0), 0, FileState.INVALID) ] as Set)
        1 * log.error(*_) >> { args ->
            assert MessageFormatter.arrayFormat(args[0], args[1]).message ==
                    "/base/storage.txt:2: couldn't decode 'this-one-errors': Invalid number of elements"
        }
    }

    def 'multiple info objects for same file in updates'() {
        given:
        def store = new FileInfoStore(log, backingFile, encoder, decoder)
        when:
        store.update([ FileInfo.create(fs.getPath('foo'), FileTime.fromMillis(0), 0, FileState.FORMATTED),
                       FileInfo.create(fs.getPath('baz'), FileTime.fromMillis(0), 0, FileState.UNFORMATTED),
                       FileInfo.create(fs.getPath('foo'), FileTime.fromMillis(0), 10, FileState.INVALID)])

        then:
        def lines = Files.readAllLines(backingFile, StandardCharsets.UTF_8)
        (lines as Set).equals([ 'baz,0,0,UNFORMATTED', 'foo,0,10,INVALID'] as Set)
    }

    def 'replace existing info'() {
        given:
        def store = new FileInfoStore(log, backingFile, encoder, decoder)
        Files.write(backingFile, 'foo,0,0,FORMATTED\n'.getBytes(StandardCharsets.UTF_8))

        when:
        store.update([ FileInfo.create(fs.getPath('foo'), FileTime.fromMillis(10), 10, FileState.UNFORMATTED) ])

        then:
        def lines = Files.readAllLines(backingFile, StandardCharsets.UTF_8)
        (lines as Set).equals([ 'foo,10000000,10,UNFORMATTED' ] as Set)
    }

    def 'create parent directories of backing file if necessary'() {
        given:
        backingFile = fs.getPath('/base/project/build/sub/states.txt')
        def store = new FileInfoStore(log, backingFile, encoder, decoder)

        expect:
        store.read().equals([] as Set)
    }
}
