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

        then: "build is successful and didn't change any files"
        result.output.contains('BUILD SUCCESSFUL')
        sampleProject.eachFileRecurse(FileType.FILES) {
            def relativePathInProject = sampleProject.toURI().relativize(it.toURI()).toString()
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
        def resultsDir = new File("src/integTest/resources/results/defaults")
        resultsDir.eachFileRecurse(FileType.FILES) {
            def relativePathInProject = resultsDir.toURI().relativize(it.toURI()).toString()
            assert it.text == new File(projectDir, relativePathInProject).text
        }

        where:
        order    | firstPluginApplied                      | secondPluginApplied
        'before' | 'com.github.sherter.google-java-format' | 'java'
        'after'  | 'java'                                  | 'com.github.sherter.google-java-format'
    }
}
