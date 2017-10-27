package com.github.sherter.googlejavaformatgradleplugin.format

import spock.lang.IgnoreIf
import spock.lang.Specification

@IgnoreIf({ javaVersion < 1.8 })
class FormatterSpec extends Specification {

    def 'AOSP-style formatting'() {
        given:
        def conf = new Configuration(version, Style.AOSP, true)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)

        expect:
        formatter.format('class Test { public   static void    main(String[] args) {     }   }') ==
                '''class Test {
                  |    public static void main(String[] args) {}
                  |}
                  |'''.stripMargin()

        where:
        version << Gjf.SUPPORTED_VERSIONS
    }

    def 'GOOGLE-style formatting'() {
        given:
        def conf = new Configuration(version, Style.GOOGLE, true)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)

        expect:
        formatter.format('class Test { public   static void    main(String[] args) {     }   }') ==
                '''class Test {
                  |  public static void main(String[] args) {}
                  |}
                  |'''.stripMargin()

        where:
        version << Gjf.SUPPORTED_VERSIONS
    }

    def 'formatter formats javadoc'() {
        given:
        def conf = new Configuration(version, Style.GOOGLE, true)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)
        def javadoc = '''/**  This is (was) malformed javadoc.
                        |*      <pre>
                        |*System.err.println
                        |*   </pre>
                        |   */
                        |'''.stripMargin()
        expect:
        formatter.format(javadoc).startsWith(
                '/**' + System.lineSeparator() +
                ' * This is (was) malformed javadoc.')

        where:
        version << Gjf.SUPPORTED_VERSIONS
    }

    def 'formatter orders imports when asked'() {
        given:
        def conf = new Configuration(version, Style.GOOGLE, true)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)
        def unordered = '''import z.Z;
                          |import a.A;
                          |
                          |class C {
                          |  A a;
                          |  Z z;
                          |}
                          |'''.stripMargin()
        def ordered = '''import a.A;
                        |import z.Z;
                        |
                        |class C {
                        |  A a;
                        |  Z z;
                        |}
                        |'''.stripMargin()
        expect:
        formatter.format(unordered) == ordered

        where:
        version << Gjf.SUPPORTED_VERSIONS
    }

    def 'formatter leaves imports alone when asked'() {
        given:
        def conf = new Configuration(version, Style.GOOGLE, false)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)
        def unordered = '''import z.Z;
                          |import a.A;
                          |
                          |class C {
                          |  A a;
                          |  Z z;
                          |}
                          |'''.stripMargin()
        def ordered = '''import z.Z;
                        |import a.A;
                        |
                        |class C {
                        |  A a;
                        |  Z z;
                        |}
                        |'''.stripMargin()
        expect:
        if (version != '1.0') {
          formatter.format(unordered) == ordered
        }

        where:
        version << Gjf.SUPPORTED_VERSIONS
    }

    def 'formatter removes unused imports'() {
        given:
        def conf = new Configuration(version, Style.GOOGLE, true)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)

        expect:
        formatter.format('''import z.Z;
                           |import a.A;
                           |
                           |class C {
                           |  A a;
                           |}
                           |'''.stripMargin()) == '''import a.A;
                                                    |
                                                    |class C {
                                                    |  A a;
                                                    |}
                                                    |'''.stripMargin()


        where:
        version << Gjf.SUPPORTED_VERSIONS - ['1.0']
    }

    def 'formatter throws when given invalid source code'() {
        given:
        def conf = new Configuration(version, Style.GOOGLE, true)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)

        when:
        formatter.format('Hello World!')
        then:
        thrown(FormatterException)

        where:
        version << Gjf.SUPPORTED_VERSIONS
    }
}
