package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Unroll

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME
import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME

@Unroll
class ExtensionSpec extends AbstractIntegrationSpec {

    def 'change default inputs in extension'() {
        given:
        def foo = project.createFile(['Foo.java'], 'class   Foo   { void bar()  {}}')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$extension
            |""".stripMargin())

        when:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        foo.read() == expected

        where:
        extension                                  | expected
        ""                                         | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { setSource 'bar' }"     | "class   Foo   { void bar()  {}}"
        "googleJavaFormat { source 'bar' }"        | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { exclude 'Foo*' }"      | "class   Foo   { void bar()  {}}"
        "googleJavaFormat { include '**/*bar*' }"  | "class   Foo   { void bar()  {}}"
    }

    def 'task exclude/includes filter default inputs further and source() overwrites them (formatting task)'() {
        given:
        def foo = project.createFile(['Foo.java'], '   class Foo {}')
        def bar = project.createFile(['src', 'Bar.java'], '   class Bar {}')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |tasks.googleJavaFormat {
            |$task
            |}
            |googleJavaFormat {
            |$extension
            |}
            |""".stripMargin())

        when:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        foo.read() == expectedFoo
        bar.read() == expectedBar

        where:
        extension              | task                               |expectedFoo        | expectedBar
        ""                     | "exclude '**/Bar.java'"            | "class Foo {}\n"  | "   class Bar {}"
        ""                     | "include 'Foo.java'"               | "class Foo {}\n"  | "   class Bar {}"
        "setSource 'Foo.java'" | ""                                 | "class Foo {}\n"  | "   class Bar {}"
        "setSource 'Foo.java'" | "source('src').include('*.java')"  | "   class Foo {}" | "class Bar {}\n"
    }

    def 'task exclude/includes filter default inputs further and source() overwrites them (verification task)'() {
        given:
        def foo = project.createFile(['Foo.java'], '   class Foo {}')
        def bar = project.createFile(['src', 'Bar.java'], '   class Bar {}')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |tasks.verifyGoogleJavaFormat {
            |$task
            |}
            |googleJavaFormat {
            |$extension
            |}
            |""".stripMargin())

        when:
        def result = runner.withArguments(DEFAULT_VERIFY_TASK_NAME).buildAndFail()

        then:
        result.output =~ /$regex1/
        !(result.output =~ /$regex2/)

        where:
        extension              | task                               | regex1      | regex2
        ""                     | "exclude '**/Bar.java'"            | "Foo.java"  | "Bar.java"
        ""                     | "include 'Foo.java'"               | "Foo.java"  | "Bar.java"
        "setSource 'Foo.java'" | ""                                 | "Foo.java"  | "Bar.java"
        "setSource 'Foo.java'" | "source('src').include('*.java')"  | "Bar.java"  | "Foo.java"
    }
}
