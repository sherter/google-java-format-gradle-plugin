package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Unroll

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME
import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME

@Unroll
class IntegrationTest extends AbstractIntegrationSpec {

    def 'verify behaviour of default format task with default settings'() {
        given:
        def buildFile = project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |""".stripMargin())
        def unformattedJavaFile = project.createFile(['src', 'main', 'java', 'Foo.java'], 'class    Foo  {  }')
        def formattedJavaFile = project.createFile(['src', 'main', 'java', 'Bar.java'], 'class Bar {}\n')

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME, '--debug').build()

        then: 'files are in expected state'
        unformattedJavaFile.read() == 'class Foo {}\n'
        !formattedJavaFile.contentHasChanged()
        !formattedJavaFile.lastModifiedTimeHasChanged()
        !buildFile.contentHasChanged()
        !buildFile.lastModifiedTimeHasChanged()

        and: 'build log is as expected'
        result.output.contains('BUILD SUCCESSFUL')
        result.output =~ /\[LIFECYCLE\].*Foo\.java: formatted successfully/
        result.output =~ /\[INFO\].*Bar\.java: UP-TO-DATE/
    }


    def "up-to-date checking"() {
        given:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME)
        def buildFile = project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '0.1-alpha'
            |}
            |""".stripMargin())

        when: "formatting task is executed on an empty project"
        def result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")

        when: "source files are added"
        project.createFile(['Foo.java'], 'class    Foo  {  }')
        project.createFile(['Bar.java'], 'class Bar {}\n')
        result = runner.build()

        then: "task is not up-to-date"
        !result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")

        when: "task is executed again"
        result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")

        when: "new java source file is added and task is executed"
        def baz = project.createFile(['Baz.java'])
        result = runner.build()

        then: "task is not up-to-date"
        !result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")

        when: "file was changed"
        baz.write('class Baz {}\n')
        result = runner.build()

        then: "task is not up-to-date"
        !result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")

        when: "nothing has changed"
        result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")

        when: "formatter tool version has changed"
        buildFile.write("""\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '1.0'
            |}
            |""".stripMargin())
        result = runner.build()

        then: "task is not up-to-date"
        !result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")

        when: "nothing has changed"
        result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")

        when: "an option has changed"
        buildFile.write("""\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '1.0'
            |  options style: 'GOOGLE'
            |}
            |""".stripMargin())
        result = runner.build()

        then:
        !result.output.contains("UP-TO-DATE")

        when: "nothing has changed"
        result = runner.build()

        then: "task is up-to-date"
        result.output.contains(":$DEFAULT_FORMAT_TASK_NAME UP-TO-DATE")
    }

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

    def "invalid input (incorrect java syntax)"() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |""".stripMargin())
        project.createFile(['Invalid.java'], 'this is not valid java syntax')

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).buildAndFail()

        then:
        result.output.contains("Detected Java syntax errors")
        result.output.contains("Invalid.java")
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
