package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.test.Project
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class AbstractIntegrationSpec extends Specification {

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

    // if a dependency is not in teskit's dependency cache, try to load it from
    // the local repository first, before reaching out to the web
    static final String defaultRepositories = '''\
        |repositories {
        |  mavenLocal()
        |  jcenter()
        |}
        |'''.stripMargin()

    @Rule TemporaryFolder temporaryFolder
    GradleRunner runner
    Project project

    def setup() {
        runner = GradleRunner.create()
                .withGradleVersion(System.properties['GRADLE_VERSION'])
                .withProjectDir(temporaryFolder.root)
        project = new Project(temporaryFolder.root)
        // workaround for dying Gradle daemons on Travis
        // see https://discuss.gradle.org/t/gradle-travis-ci/11928/9
        project.createFile(['gradle.properties'], 'org.gradle.daemon=false\n')
        additionalSetup()
    }

    void additionalSetup() {};
}
