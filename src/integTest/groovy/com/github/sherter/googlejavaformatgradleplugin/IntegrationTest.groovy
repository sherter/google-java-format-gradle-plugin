package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Unroll

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME
import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME

@Unroll
class IntegrationTest extends AbstractIntegrationSpec {

    def "exclude a file from the default format task"() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |tasks.$DEFAULT_FORMAT_TASK_NAME {
            |  exclude '**/Bar.java'
            |}
            |""".stripMargin())
        def foo = project.createFile(['Foo.java'], 'class  Foo  {   }')
        def bar = project.createFile(['Bar.java'], 'class  Bar  {   }')

        when: "formatting task is executed"
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then: "source files are formatted properly afterwards"
        foo.contentHasChanged()
        foo.lastModifiedTimeHasChanged()
        !bar.contentHasChanged()
        !bar.lastModifiedTimeHasChanged()
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "define additional format task"() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |task customFormatTask(type: $GoogleJavaFormat.name) {
            |  source 'src/Foo.java'
            |}
            |""".stripMargin())
        def foo = project.createFile(['src', 'Foo.java'], 'class  Foo  {   }')
        def bar = project.createFile(['Bar.java'], 'class   Bar {  }')

        when: "formatting task is executed"
        def result = runner.withArguments('customFormatTask').build()

        then: "source files are formatted properly afterwards"
        foo.read() == 'class Foo {}\n'
        !bar.contentHasChanged()
        !bar.lastModifiedTimeHasChanged()
        result.output.contains('BUILD SUCCESSFUL')
    }

    def 'report unformatted java sources'() {
        given: "a java project with java plugin applied"
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |""".stripMargin())
        project.createFile(['Foo.java'], 'class Foo {}\n')
        project.createFile(['Bar.java'], 'class    Bar   {  }')

        when:
        def result = runner.withArguments(DEFAULT_VERIFY_TASK_NAME).buildAndFail()

        then:
        !result.output.contains('Foo.java')
        result.output.contains('Bar.java')
    }
}
