package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class AbstractIntegrationTest extends Specification {

    static final buildScriptBlock = """\
            buildscript {
                repositories {
                    mavenLocal()
                }
                dependencies {
                    classpath group: 'com.github.sherter.googlejavaformatgradleplugin',
                               name: 'google-java-format-gradle-plugin',
                            version: '${GoogleJavaFormatPlugin.PLUGIN_VERSION}'
                }
            }
            """.stripIndent()

    @Rule TemporaryFolder temporaryFolder
    File projectDir
    File buildFile
    GradleRunner runner

    def setup() {
        projectDir = temporaryFolder.root
        buildFile = new File(projectDir, 'build.gradle')
        buildFile << buildScriptBlock
        runner = GradleRunner.create().withProjectDir(projectDir).withGradleVersion(System.properties['GRADLE_VERSION'])
        customSetup()
    }

    void customSetup() {}
}
