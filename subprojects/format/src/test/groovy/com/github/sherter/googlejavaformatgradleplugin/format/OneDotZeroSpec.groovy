package com.github.sherter.googlejavaformatgradleplugin.format

import spock.lang.Specification

import static com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption.*
import static com.github.sherter.googlejavaformatgradleplugin.format.Resolver.resolve

class OneDotZeroSpec extends Specification {

    static final String version = '1.0'

    def 'formatter without options formats valid source code'() {
        given:
        def formatter = Gjf.newFormatter(resolve(version), version)

        expect:
        'class Test {}\n' == formatter.format('class Test {}')
    }

    def 'formatter throws when given invalid source code'() {
        given:
        def formatter = Gjf.newFormatter(resolve(version), version)

        when:
        formatter.format('Hello World!')

        then:
        thrown(FormatterException)
    }

    def 'formatter with AOSP style'() {
        given:
        def formatter = Gjf.newFormatter(resolve(version), version, AOSP_STYLE)

        expect:
        formatter.format('class Test { public   static void    main(String[] args) {     }   }') ==
                '''class Test {
                |    public static void main(String[] args) {}
                |}
                |'''.stripMargin()
    }

    def 'formatter with GOOGLE style'() {
        given:
        def formatter = Gjf.newFormatter(resolve(version), version, GOOGLE_STYLE)

        expect:
        formatter.format('class Test { public   static void    main(String[] args) {     }   }') ==
                '''class Test {
                |  public static void main(String[] args) {}
                |}
                |'''.stripMargin()
    }

    def 'test javadoc formatter options'() {
        given:
        def formatterWithout = Gjf.newFormatter(resolve(version), version, NO_JAVADOC_FORMATTER)
        def formatterEclipse = Gjf.newFormatter(resolve(version), version, ECLIPSE_JAVADOC_FORMATTER)
        def javadoc = '''/**  This is (was) malformed javadoc.
                         |*      <pre>
                         |*System.err.println
                         |*   </pre>
                         |   */
                         |'''.stripMargin()

        expect:
        formatterWithout.format(javadoc) != formatterEclipse.format(javadoc)
    }

    def 'construct formatter with conflicting options throws'() {
        when:
        Gjf.newFormatter(resolve(version), version, NO_JAVADOC_FORMATTER, ECLIPSE_JAVADOC_FORMATTER)

        then:
        IllegalArgumentException e = thrown()
        e.message.contains('Conflict')
        e.message.contains(NO_JAVADOC_FORMATTER.name())
        e.message.contains(ECLIPSE_JAVADOC_FORMATTER.name())

        when:
        Gjf.newFormatter(resolve(version), version, GOOGLE_STYLE, AOSP_STYLE)

        then:
        e = thrown()
        e.message.contains('Conflict')
        e.message.contains(GOOGLE_STYLE.name())
        e.message.contains(AOSP_STYLE.name())
    }

    def 'construction with no options fails if classloader provides wrong class versions'() {
        when:
        Gjf.newFormatter(resolve('0.1-alpha'), version)

        then:
        IllegalArgumentException e = thrown()
        e.message.contains('Invalid ClassLoader')
    }
}
