package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.test.FileWithState

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME
import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME

class MultiprojectSpec extends AbstractIntegrationSpec {

    FileWithState buildfile

    void additionalSetup() {
        buildfile = project.createFile(['build.gradle'], """\
            |$buildScriptBlock
            |
            |subprojects {
            |  apply plugin: 'com.github.sherter.google-java-format'
            |  $defaultRepositories
            |  $downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |  tasks.verifyGoogleJavaFormat.ignoreFailures = true
            |}
            |""".stripMargin())
        project.createFile(['settings.gradle'], """\
            |include 'sub2', 'sub1', 'sub3'
            |""".stripMargin())
    }


    def "multiproject format"() {
        project.createFile(['sub1', 'Foo1.java'], 'class Foo {   } ')
        project.createFile(['sub2', 'Foo2.java'], 'class Foo {   } ')
        project.createFile(['sub3', 'Foo3.java'], 'class Foo {   } ')

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output.contains('sub1/Foo1.java')
        result.output.contains('sub2/Foo2.java')
        result.output.contains('sub3/Foo3.java')
    }

    def "multiproject verify"() {
        project.createFile(['sub1', 'Foo1.java'], 'class Foo {   } ')
        project.createFile(['sub2', 'Foo2.java'], 'class Foo {   } ')
        project.createFile(['sub3', 'Foo3.java'], 'class Foo {   } ')

        when:
        def result = runner.withArguments(DEFAULT_VERIFY_TASK_NAME).build()

        then:
        result.output.contains('sub1/Foo1.java')
        result.output.contains('sub2/Foo2.java')
        result.output.contains('sub3/Foo3.java')
    }
}
