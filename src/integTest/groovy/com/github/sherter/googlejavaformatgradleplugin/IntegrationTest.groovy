package com.github.sherter.googlejavaformatgradleplugin

import groovy.io.FileType
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class IntegrationTest extends AbstractIntegrationTest {

    @Shared File sampleProject = new File('src/integTest/resources/project')

    @Override
    void customSetup() {
        runner.withArguments(GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME, '--stacktrace')
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


    def "up-to-date checking"() {
        given:
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'com.github.sherter.google-java-format'

            repositories {
                maven {
                    url 'https://oss.sonatype.org/content/repositories/snapshots/'
                }
                jcenter()
            }
            """

        when: "formatting task is executed on an empty project"
        def result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} UP-TO-DATE")

        when: "source files are added"
        new AntBuilder().copy(todir: projectDir) { fileset(dir: sampleProject) }
        result = runner.build()

        then: "task is not up-to-date"
        !result.output.contains(":${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} UP-TO-DATE")

        when: "task is executed again"
        result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} UP-TO-DATE")

        when: "new java source file is added and task is executed"
        def newSource = new File(projectDir, 'src/main/java/NewJavaSource.java')
        newSource.createNewFile()
        result = runner.build()

        then: "task is not up-to-date"
        !result.output.contains(":${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} UP-TO-DATE")

        when: "file was changed"
        newSource << "class NewJavaSource {}"
        result = runner.build()

        then: "task is not up-to-date"
        !result.output.contains(":${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} UP-TO-DATE")

        when: "nothing has changed"
        result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} UP-TO-DATE")

        when: "formatter tool version has changed"
        buildFile << """
            googleJavaFormat {
                toolVersion = '0.1-SNAPSHOT'
            }
            """
        result = runner.build()

        then: "task is not up-to-date"
        !result.output.contains(":${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} UP-TO-DATE")

        when: "nothing has changed"
        result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} UP-TO-DATE")
    }

    def "include and exclude sources"() {
        given:
        new AntBuilder().copy(todir: projectDir) { fileset(dir: sampleProject) }
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'com.github.sherter.google-java-format'

            repositories {
                jcenter()
            }
            tasks.${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME} {
                source 'src'
                include '**/*.java'
                exclude '**/Bar.java'
            }
            """

        when: "formatting task is executed"
        def result = runner.build()

        then: "source files are formatted properly afterwards"
        result.output.contains('BUILD SUCCESSFUL')
        sameFilesExistAndHaveSameContent(new File("src/integTest/resources/results/include-exclude"))
    }

    def "define additional format task"() {
        given:
        new AntBuilder().copy(todir: projectDir) { fileset(dir: sampleProject) }
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'com.github.sherter.google-java-format'

            repositories {
                jcenter()
            }

            task customFormatTask(type: com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormat) {
                source 'src/Foo.java'
            }
            """

        when: "formatting task is executed"
        def result = runner.withArguments('customFormatTask').build()

        then: "source files are formatted properly afterwards"
        result.output.contains('BUILD SUCCESSFUL')
        sameFilesExistAndHaveSameContent(new File("src/integTest/resources/results/custom"))
    }

    def "invalid input (incorrect java syntax)"() {
        given:
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'com.github.sherter.google-java-format'

            repositories {
                maven {
                    url 'https://oss.sonatype.org/content/repositories/snapshots/'
                }
                jcenter()
            }

            tasks."${GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME}" {
                source file('Invalid.java')
            }
            """
        def inputFile = new File(projectDir, 'Invalid.java')
        inputFile.createNewFile()
        inputFile << "<< -.- \$asd\$This is not a valid java class!"

        when:
        def result = runner.build()

        then:
        result.output.contains("$inputFile is not a valid Java source file")
    }

    def 'report unformatted java sources'() {
        given: "a java project with java plugin applied"
        new AntBuilder().copy(todir: projectDir) { fileset(dir: sampleProject) }
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'com.github.sherter.google-java-format'

            repositories {
                jcenter()
            }
            """

        when:
        def result = runner.withArguments(GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME)
                .buildAndFail()

        then:
        result.output.contains('Bar.java')
    }
}
