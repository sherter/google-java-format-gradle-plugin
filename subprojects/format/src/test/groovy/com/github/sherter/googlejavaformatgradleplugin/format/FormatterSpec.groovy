package com.github.sherter.googlejavaformatgradleplugin.format

import spock.lang.Specification

class FormatterSpec extends Specification {

    def 'formatter formats valid source code'() {
        given:
        def conf = new Configuration(version, Style.GOOGLE)
        def formatter = Gjf.newFormatter(Resolver.resolve(version), conf)

        expect:
        formatter.format('class Test {}') == 'class Test {}' + System.lineSeparator()

        where:
        version << ['1.0']
    }

    def 'formatter orders imports'() {
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
        version << ['1.0']
    }
}
