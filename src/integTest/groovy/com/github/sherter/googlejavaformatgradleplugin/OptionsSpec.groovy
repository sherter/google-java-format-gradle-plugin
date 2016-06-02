package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.test.Project
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class OptionsSpec extends Specification {

    @Rule TemporaryFolder temporaryFolder
    GradleRunner runner
    Project project

    def setup() {
        runner = GradleRunner.create().withProjectDir(temporaryFolder.root).withGradleVersion(System.properties['GRADLE_VERSION'])
        project = new Project(temporaryFolder.root)
    }

    def 'format and verify using different styles'() {
        given:
        project.createFile(['build.gradle'], """\
            |${AbstractIntegrationTest.buildScriptBlock}
            |apply plugin: 'com.github.sherter.google-java-format'
            |
            |repositories {
            |  jcenter()
            |}
            |$options
            |""".stripMargin())
        def foo = project.createFile(['Foo.java'], 'class   Foo   { void bar()  {}}')

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
        options                                        | expected
        ""                                             | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { options style: 'GOOGLE' }" | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { options style: 'AOSP' }"   | "class Foo {\n    void bar() {}\n}\n"
    }

    def 'javadoc is formatted with expected javadoc formatter'() {
        given:
        project.createFile(['build.gradle'], """\
            |${AbstractIntegrationTest.buildScriptBlock}
            |apply plugin: 'com.github.sherter.google-java-format'
            |
            |repositories {
            |  jcenter()
            |}
            |
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
            |${AbstractIntegrationTest.buildScriptBlock}
            |apply plugin: 'com.github.sherter.google-java-format'
            |
            |repositories {
            |  jcenter()
            |}
            |
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
        runner.withArguments('goJF', '--stacktrace').build()

        then:
        foo.read() == expected

        where:
        // TODO(sherter): add another row as soon as a version is released that actually supports it
        toolVersion    | expected
        '0.1-alpha'    | 'import second.Foo;\nimport first.Bar;\n\nclass Foo {}\n' // no sorting
        '1.0'          | 'import second.Foo;\nimport first.Bar;\n\nclass Foo {}\n' // no sorting (google-java-format issue #42)
    }
}
