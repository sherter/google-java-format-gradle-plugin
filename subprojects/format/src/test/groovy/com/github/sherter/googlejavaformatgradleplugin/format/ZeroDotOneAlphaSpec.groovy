package com.github.sherter.googlejavaformatgradleplugin.format

import spock.lang.Specification

import static com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption.*

class ZeroDotOneAlphaSpec extends Specification {

    static final String version = '0.1-alpha'

    def 'formatter formats valid source code'() {
        given:
        def formatter = Gjf.newFormatter(Resolver.resolve(version), version)

        expect:
        'class Test {}\n' == formatter.format('class Test {}')
    }

    def 'formatter throws when given invalid source code'() {
        given:
        def formatter = Gjf.newFormatter(Resolver.resolve(version), version)

        when:
        formatter.format('Hello World!')

        then:
        thrown(FormatterException)
    }

    def 'construction fails if any formatter option is provided'() {
        when:
        Gjf.newFormatter(Resolver.resolve(version), version, option)

        then:
        IllegalArgumentException e = thrown()
        e.message.contains('Unsupported option')
        e.message.contains(option.toString())

        where:
        option << values()
    }

    def 'construction fails if multiple formatter options are provided'() {
        when:
        Gjf.newFormatter(Resolver.resolve(version), version, *options)

        then:
        IllegalArgumentException e = thrown()
        e.message.contains('Unsupported options')

        where:
        options << [[AOSP_STYLE, ECLIPSE_JAVADOC_FORMATTER],
                    [GOOGLE_STYLE, NO_JAVADOC_FORMATTER],
                    [AOSP_STYLE, NO_JAVADOC_FORMATTER]]
    }

    def 'construction fails if provided classloader is invalid'() {
        when:
        Gjf.newFormatter(Resolver.resolve('1.0'), version)

        then:
        IllegalArgumentException e = thrown()
        e.message.contains('Invalid ClassLoader')
    }
}
