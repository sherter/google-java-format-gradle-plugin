package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.test.FileWithStateFactory
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class OptionsSpec extends Specification {

    @Rule TemporaryFolder temporaryFolder
    GradleRunner runner
    FileWithStateFactory create

    def setup() {
        runner = GradleRunner.create().withProjectDir(temporaryFolder.root).withGradleVersion(System.properties['GRADLE_VERSION'])
        create = new FileWithStateFactory(temporaryFolder.root)
    }

    def 'format and verify using different styles'() {
        given:
        def buildfile = create.file(['build.gradle'], """\
            |${AbstractIntegrationTest.buildScriptBlock}
            |apply plugin: 'com.github.sherter.google-java-format'
            |
            |repositories {
            |  jcenter()
            |}
            |$options
            |""".stripMargin())
        def foo = create.file(['Foo.java'], 'class   Foo   { void bar()  {}}')

        when:
        runner.withArguments('verGJF').build()

        then:
        thrown(UnexpectedBuildFailure)

        when:
        def result = runner.withArguments('goJF').build()

        then:
        new String(foo.content(), StandardCharsets.UTF_8) == expected

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
}
