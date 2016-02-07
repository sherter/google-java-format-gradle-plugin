package com.github.sherter.googlejavaformatgradleplugin

import groovy.io.FileType
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class IntegrationTest extends Specification {

    @Shared File sampleProject = new File('src/integTest/resources/project')

    @Rule TemporaryFolder temporaryFolder
    File projectDir
    File buildFile
    GradleRunner runner

    def setup() {
        projectDir = temporaryFolder.root
        buildFile = new File(projectDir, 'build.gradle')
        buildFile << """
            buildscript {
                repositories {
                    mavenLocal()
                }
                dependencies {
                    classpath group:   'com.github.sherter.googlejavaformatgradleplugin',
                              name:    'google-java-format-gradle-plugin',
                              version: '${System.getProperty('plugin-version')}'
                }
            }
            """
        runner = GradleRunner.create()
                .withGradleVersion('2.0')
                .withProjectDir(projectDir)
                .withArguments(GoogleJavaFormatPlugin.TASK_NAME)
    }

    def "apply plugin to project with no source sets"() {
        given: "a project where only our plugin is applied"
        new AntBuilder().copy(todir: projectDir) { fileset(dir: sampleProject) }
        buildFile << "apply plugin: 'com.github.sherter.google-java-format'"

        when: "format task is run"
        def result = runner.build()

        then: "build is UP-TO-DATE"
        result.output.contains(":${GoogleJavaFormatPlugin.TASK_NAME} UP-TO-DATE")
        result.output.contains('BUILD SUCCESSFUL')
        sameFilesExistAndHaveSameContent(sampleProject)
    }

    void sameFilesExistAndHaveSameContent(File expectedDir) {
        expectedDir.eachFileRecurse(FileType.FILES) {
            def relativePathInProject = expectedDir.toURI().relativize(it.toURI()).toString()
            assert it.text == new File(projectDir, relativePathInProject).text
        }
    }

    def "format with default settings and plugin applied #order java plugin"() {
        given: "a java project with java plugin applied"
        new AntBuilder().copy(todir: projectDir) { fileset(dir: sampleProject) }
        buildFile << """
            apply plugin: '${firstPluginApplied}'
            apply plugin: '${secondPluginApplied}'

            repositories {
                jcenter()
            }
            """

        when: "formatting task is executed"
        def result = runner.build()

        then: "source files are formatted properly afterwards"
        result.output.contains('BUILD SUCCESSFUL')
        sameFilesExistAndHaveSameContent(new File("src/integTest/resources/results/defaults"))

        where:
        order    | firstPluginApplied                      | secondPluginApplied
        'before' | 'com.github.sherter.google-java-format' | 'java'
        'after'  | 'java'                                  | 'com.github.sherter.google-java-format'
    }
}
