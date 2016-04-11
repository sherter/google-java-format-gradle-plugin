package com.github.sherter.googlejavaformatgradleplugin

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

class FileToStateMapperTest extends Specification {

    def fs = Jimfs.newFileSystem(
            Configuration.forCurrentPlatform()
                    .toBuilder()
                    .setWorkingDirectory('/work')
                    .build())
    def mapper = new FileToStateMapper()

    def 'contains newest result after putting multiple results in parallel'() {
        given:
        def resultList = (1..100000).collect {
            FileTime randomFileTime = FileTime.fromMillis(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
            FileInfo.create(fs.getPath('foo'), randomFileTime, 0L, FileState.FORMATTED)
        }
        def newestResult = resultList.max { result -> result.lastModified().toMillis() }
        def executor = Executors.newFixedThreadPool(20)

        when:
        resultList.each { result ->
            executor.execute({ mapper.putIfNewer(result) } as Runnable)
        }
        executor.shutdown()
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)

        then:
        mapper.get(fs.getPath('foo').toAbsolutePath()) == newestResult
    }

    def 'get non existing file info'() {
        expect:
        mapper.get(fs.getPath('foo')) == null
    }

    def 'get file info for relative path returns info for equivalent absolute and normalized path'() {
        given:
        def path = fs.getPath('foo')
        def info = FileInfo.create(path, FileTime.fromMillis(0), 0, FileState.FORMATTED)

        when:
        mapper.putIfNewer(info)

        then:
        mapper.get(path).equals(info)
    }

    def 'map unknown path to file state'() {
        expect:
        mapper.map(fs.getPath('')) == FileState.UNKNOWN
    }

    def 'map path to its known state'() {
        given:
        def path = fs.getPath('foo')
        Files.createFile(path)
        Files.setLastModifiedTime(path, FileTime.fromMillis(0))

        when:
        mapper.putIfNewer(FileInfo.create(path, FileTime.fromMillis(0), 0, FileState.FORMATTED))

        then:
        mapper.map(fs.getPath('../work/foo')) == FileState.FORMATTED

        when:
        def bytes = 'content'.getBytes(StandardCharsets.UTF_8)
        Files.write(path, bytes)
        def time = Files.getLastModifiedTime(path)
        mapper.putIfNewer(FileInfo.create(path, time, bytes.length, FileState.FORMATTED))

        then:
        mapper.map(path) == FileState.FORMATTED
    }

    def 'map path to state when existing file info is outdated'() {
        given:
        def path = fs.getPath('foo')
        Files.createFile(path)
        Files.setLastModifiedTime(path, FileTime.fromMillis(1))

        when: "last modified times don't match"
        def info = FileInfo.create(path, FileTime.fromMillis(0), 0, FileState.FORMATTED)
        mapper.putIfNewer(info)
        assert mapper.get(path) == info

        then:
        mapper.map(path) == FileState.UNKNOWN
        mapper.get(path) == null

        when: "file sizes don't match"
        info = FileInfo.create(path, FileTime.fromMillis(1), 10, FileState.FORMATTED)
        mapper.putIfNewer(info)
        assert mapper.get(path) == info

        then:
        mapper.map(path) == FileState.UNKNOWN
        mapper.get(path) == null
    }

    def 'reverse map contains all paths'() {
        given:
        def paths = [ fs.getPath('foo'), fs.getPath('foo').toAbsolutePath(), fs.getPath('bar') ]

        when:
        def multimap = mapper.reverseMap(paths)

        then:
        multimap.keySet().size() == 1
        multimap.containsKey(FileState.UNKNOWN)
        (multimap.get(FileState.UNKNOWN) as Set).equals(paths as Set)
    }
}
