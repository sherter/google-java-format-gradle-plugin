package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.testkit.runner.UnexpectedBuildFailure

class OptionsSpec extends AbstractIntegrationSpec {

    def 'format and verify using different styles'() {
        given:
        def foo = project.createFile(['Foo.java'], 'class   Foo   { void bar()  {}}')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$extension
            |""".stripMargin())

        when:
        runner.withArguments('verGJF').build()

        then:
        thrown(UnexpectedBuildFailure)

        when:
        runner.withArguments('goJF').build()

        then:
        foo.lastModifiedTimeHasChanged()
        foo.contentHasChanged()
        foo.read() == expected

        when:
        runner.withArguments('verGJF').build()

        then:
        notThrown(UnexpectedBuildFailure)

        where:
        extension                                      | expected
        ""                                             | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { options style: 'GOOGLE' }" | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { options style: 'AOSP' }"   | "class Foo {\n    void bar() {}\n}\n"
    }

    def 'javadoc is formatted with expected javadoc formatter'() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '$toolVersion'
            |}
            |""".stripMargin())
        def foo = project.createFile(['Foo.java'], '''\
            |/**
            | * foo
            | * bar
            | */
            |class Foo {}
            |'''.stripMargin())

        when:
        runner.withArguments('goJF').build()

        then:
        foo.read() == expected

        where:
        toolVersion | expected
        '0.1-alpha' | '/**\n * foo\n * bar\n */\nclass Foo {}\n' // no javadoc formatting
        '1.0'       | '/**\n * foo bar\n */\nclass Foo {}\n' // EclipseJavadocFormatter
    }

    def 'imports are sorted if supported by tool version'() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '$toolVersion'
            |}
            |""".stripMargin())
        def foo = project.createFile(['Foo.java'], '''\
            |import second.Foo;
            |import first.Bar;
            |
            |class Foo {}
            |'''.stripMargin())

        when:
        runner.withArguments('goJF').build()

        then:
        foo.read() == expected

        where:
        // TODO(sherter): add another row as soon as a version is released that actually supports it
        toolVersion    | expected
        '0.1-alpha'    | 'import second.Foo;\nimport first.Bar;\n\nclass Foo {}\n' // no sorting
        '1.0'          | 'import second.Foo;\nimport first.Bar;\n\nclass Foo {}\n' // no sorting (google-java-format issue #42)
    }
}
