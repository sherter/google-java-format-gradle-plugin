package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Specification

class ExtensionSpec extends Specification {

    def ext = new GoogleJavaFormatExtension()
    def extClosure = ext.&with

    def 'default tool version is returned if none is set explicitly'() {
        expect:
        ext.toolVersion == GoogleJavaFormatExtension.DEFAULT_TOOL_VERSION
        ext.@toolVersion == null
    }

    def 'empty options are returned if none were set'() {
        expect:
        ext.options == [:]
        ext.@options == null
    }

    def 'returned options are immutable'() {
        when:
        ext.options.put('', '')

        then:
        thrown(UnsupportedOperationException)

        when:
        extClosure {
            options javadoc: true
        }

        then:
        ext.options.containsKey('javadoc')

        when:
        ext.options.put('baz', 'foo')

        then:
        thrown(UnsupportedOperationException)
    }

    def 'setting options directly throws'() {
        when:
        ext.options = [foo: 'bar', baz: 'foo']

        then:
        ConfigurationException e = thrown()
        e.message.contains('Not allowed')
    }

    def 'set tool version twice throws'() {
        when:
        extClosure {
            toolVersion = '1.0'
            toolVersion = '0.1-alpha'
        }

        then:
        ConfigurationException e = thrown()
        e.message.matches(/.*toolVersion.*twice.*/)
    }

    def 'set tool version after adding options throws'() {
        when:
        extClosure {
            options style: 'GOOGLE', javadoc: false
            toolVersion = '1.0'
        }

        then:
        ConfigurationException e = thrown()
        e.message.matches(/.*toolVersion.*before.*options.*/)
    }

    def 'overwrite previously set option throws'() {
        when:
        extClosure {
            options style: 'GOOGLE'
            options style: 'AOSP'
        }

        then:
        ConfigurationException e = thrown()
        e.message.matches(/.*style.*twice.*/)
    }
}
