package com.github.sherter.googlejavaformatgradleplugin.format


import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.Jvm

import static org.junit.Assume.assumeTrue

@Unroll
class FormatterSpec extends Specification {

    def 'AOSP-style formatting (v#version)'() {
        skipIfJvmNotSupported(version)
        given:
        def conf = new Configuration(version, Style.AOSP)
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

    def 'GOOGLE-style formatting (v#version)'() {
        skipIfJvmNotSupported(version)
        given:
        def conf = new Configuration(version, Style.GOOGLE)
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

    def 'formatter formats javadoc (v#version)'() {
        skipIfJvmNotSupported(version)
        given:
        def conf = new Configuration(version, Style.GOOGLE)
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

    def 'formatter orders imports (v#version)'() {
        skipIfJvmNotSupported(version)
        given:
        def conf = new Configuration(version, Style.GOOGLE)
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

    def 'formatter removes unused imports (v#version)'() {
        skipIfJvmNotSupported(version)
        given:
        def conf = new Configuration(version, Style.GOOGLE)
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

    def 'formatter throws when given invalid source code (v#version)'() {
        skipIfJvmNotSupported(version)
        given:
        def conf = new Configuration(version, Style.GOOGLE)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)

        when:
        formatter.format('Hello World!')
        then:
        thrown(FormatterException)

        where:
        version << Gjf.SUPPORTED_VERSIONS
    }

    private static skipIfJvmNotSupported(version) {
        assumeTrue(Gjf.SUPPORTED_VERSIONS.indexOf(version) < Gjf.SUPPORTED_VERSIONS.indexOf('1.8') ||
                new BigDecimal(Jvm.current.javaSpecificationVersion) >= BigInteger.valueOf(11))
    }
}
