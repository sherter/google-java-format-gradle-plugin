package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.test.Project
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class AbstractIntegrationTest extends Specification {

    static final String buildScriptBlock = """\
        |buildscript {
        |  repositories {
        |    mavenLocal()
        |  }
        |  dependencies {
        |    classpath group: 'com.github.sherter.googlejavaformatgradleplugin',
        |               name: 'google-java-format-gradle-plugin',
        |            version: '${GoogleJavaFormatPlugin.PLUGIN_VERSION}'
        |  }
        |}
        |""".stripMargin()

    static final String applyPlugin = """\
        |$buildScriptBlock
        |apply plugin: 'com.github.sherter.google-java-format'
        |""".stripMargin()

    @Rule TemporaryFolder temporaryFolder
    File projectDir
    File buildFile
    GradleRunner runner
    Project project

    def setup() {
        projectDir = temporaryFolder.root
        buildFile = new File(projectDir, 'build.gradle')
        runner = GradleRunner.create().withProjectDir(projectDir).withGradleVersion(System.properties['GRADLE_VERSION'])
        project = new Project(projectDir)
        customSetup()
    }

    void customSetup() {}
}
