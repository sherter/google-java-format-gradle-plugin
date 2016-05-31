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

    def 'style option'() {
        given:
        def buildfile = create.file(['build.gradle'], """\
            |${AbstractIntegrationTest.buildScriptBlock}
            |apply plugin: 'com.github.sherter.google-java-format'
            |
            |repositories {
            |  jcenter()
            |}
            |
            |googleJavaFormat {
            |  options style: 'GOOGLE'
            |}
            |""".stripMargin())
        def foo = create.file(['Foo.java'], 'class   Foo   { public static void main(String[] args)   {}}')

        when:
        runner.withArguments('verGJF').build()

        then:
        thrown(UnexpectedBuildFailure)

        when:
        runner.withArguments('goJF').build()
        runner.withArguments('verGJF').build()

        then:
        notThrown(UnexpectedBuildFailure)
        println new String(foo.content(), StandardCharsets.UTF_8)

        when:
        buildfile.write("""\
            |${AbstractIntegrationTest.buildScriptBlock}
            |apply plugin: 'com.github.sherter.google-java-format'
            |
            |repositories {
            |  jcenter()
            |}
            |
            |googleJavaFormat {
            |  options style: 'AOSP'
            |}
            |""".stripMargin())
        runner.withArguments('verGJF').build()

        then:
        thrown(UnexpectedBuildFailure)

        when:
        runner.withArguments('goJF').build()
        runner.withArguments('verGJF').build()
        println new String(foo.content(), StandardCharsets.UTF_8)

        then:
        notThrown(UnexpectedBuildFailure)
    }
}
