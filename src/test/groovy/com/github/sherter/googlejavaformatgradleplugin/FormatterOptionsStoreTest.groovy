package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.Style
import com.google.common.collect.ImmutableSet
import com.google.common.jimfs.Jimfs
import spock.lang.Specification

import java.nio.file.Files

class FormatterOptionsStoreTest extends Specification {

    def fs = Jimfs.newFileSystem()
    def backingFile = fs.getPath('settings')


    def 'read valid options from store'() {
        given:
        Files.write(backingFile, 'toolVersion=123'.getBytes('UTF-8'))
        def store = new FormatterOptionsStore(backingFile);

        when:
        def options = store.read()

        then:
        options.version() == '123'
    }

    def 'read from non-existing file'() {
        given:
        def store = new FormatterOptionsStore(backingFile);

        when:
        def options = store.read()

        then:
        options.version() == null
    }

    def 'read options equal written options'() {
        given:
        def store = new FormatterOptionsStore(backingFile);

        when:
        store.write(option)

        then:
        store.read() == option

        where:
        option << [FormatterOptions.create('0.1-alpha', Style.GOOGLE),
                   FormatterOptions.create('1.0', Style.AOSP),
                   FormatterOptions.create('-123', Style.GOOGLE)]
    }

}
