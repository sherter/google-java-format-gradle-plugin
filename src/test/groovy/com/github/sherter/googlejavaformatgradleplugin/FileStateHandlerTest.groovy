package com.github.sherter.googlejavaformatgradleplugin

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class FileStateHandlerTest extends Specification {

    @Rule TemporaryFolder temporaryFolder
    File projectDir
    File buildCacheDir

    def setup() {
        projectDir = temporaryFolder.root
        buildCacheDir = new File(temporaryFolder.root, 'build/gjf/xyz')
    }

    def "load corrupted file state database"() {
        def handler = new FileStateHandler(projectDir, buildCacheDir, 'version')
        def statesFile = new File(buildCacheDir, FileStateHandler.stateFileName)
        buildCacheDir.mkdirs()
        statesFile << "I won't deserialize to a Map!"

        when:
        handler.load()

        then: "corrupted file is deleted"
        !statesFile.exists()
    }

    def "second call to formatIfNotUpToDate doesn't re-format"() {
        def handler = new FileStateHandler(projectDir, buildCacheDir, 'version')
        def file = new File(projectDir, 'file')
        file << "put something in there..."

        when:
        def newContent = "That's how I look like after formatting..."
        handler.formatIfNotUpToDate(file) {
            it.text = newContent
        }

        then:
        handler.formatIfNotUpToDate(file) {
            assert false
        }
        file.text == newContent
    }

    def "file is up-to-date after formatting"() {
        def handler = new FileStateHandler(projectDir, buildCacheDir, 'version')

        when:
        def file = new File(projectDir, 'file')
        file << "put something in there..."

        then:
        !handler.isUpToDate(file)

        when:
        def newContent = "That's how I look like after formatting..."
        handler.formatIfNotUpToDate(file) {
            it.text = newContent
        }

        then:
        handler.isUpToDate(file)

        when:
        file << "changing it from outside again..."

        then:
        !handler.isUpToDate(file)
    }

    def "file is up-to-date after flush and load"() {
        def handler = new FileStateHandler(projectDir, buildCacheDir, 'version')
        def file = new File(projectDir, 'file')
        file << "put something in there..."
        def formattedContent = "that's the right style!"
        handler.formatIfNotUpToDate(file) {
            file.text = formattedContent
        }
        handler.flush()

        when:
        handler = new FileStateHandler(projectDir, buildCacheDir, 'version')

        then:
        !handler.isUpToDate(file)

        when:
        handler.load()

        then:
        handler.isUpToDate(file)
    }


    def "file is no longer up-to-date when google-java-format version changed"() {
        def handler = new FileStateHandler(projectDir, buildCacheDir, 'v1')
        def file = new File(projectDir, 'file')
        file << "put something in there..."
        def formattedContent = "that's the right style!"
        handler.formatIfNotUpToDate(file) {
            file.text = formattedContent
        }
        handler.flush()

        when:
        handler = new FileStateHandler(projectDir, buildCacheDir, 'v2')
        handler.load()

        then:
        !handler.isUpToDate(file)
    }
}
