package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.test.FileWithState

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME
import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME

class UpToDateSpec extends AbstractIntegrationSpec {

    FileWithState buildfile

    void additionalSetup() {
        buildfile = project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |""".stripMargin())
    }

    def "format task is up-to-date in an empty project"() {
        expect:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build().output =~ /UP-TO-DATE/
    }

    def "format task is up-to-date when executed the second time"() {
        project.createFile(['Foo.java'], 'class  Foo {  }')

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        !(result.output =~ /UP-TO-DATE/)

        when:
        result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /UP-TO-DATE/
    }

    def "format task is up-to-date when verification task succeeded before"() {
        project.createFile(['Foo.java'], 'class Foo {}\n')

        when:
        def result = runner.withArguments(DEFAULT_VERIFY_TASK_NAME).build()

        then:
        result.output =~ /BUILD SUCCESSFUL/

        when:
        result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /UP-TO-DATE/
    }

    def "format task is not up-to-date anymore, if a new file was added"() {
        project.createFile(['Foo.java'], 'class Foo {}\n')

        when:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /UP-TO-DATE/

        when:
        project.createFile(['Bar.java'], 'class Bar {}\n')
        result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        !(result.output =~ /UP-TO-DATE/)
    }

    def "format task is not up-to-date anymore, if a file was modified"() {
        def foo = project.createFile(['Foo.java'], 'class Foo {}\n')

        when:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /UP-TO-DATE/

        when:
        foo.write('class Foo { void bar() {} }')
        result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        !(result.output =~ /UP-TO-DATE/)
    }

    def "format task is still up-to-date after removing a file"() {
        def foo = project.createFile(['Foo.java'], 'class Foo {}\n')

        when:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /UP-TO-DATE/

        when:
        foo.delete()
        result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /UP-TO-DATE/
    }

    def "format task is not up-to-date anymore, if the toolVersion has changed"() {
        project.createFile(['Foo.java'], 'class Foo {}')
        buildfile.write("""\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '1.0'
            |}
            |""".stripMargin())

        when:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /UP-TO-DATE/

        when:
        buildfile.write("""\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '0.1-alpha'
            |}
            |""".stripMargin())
        result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        !(result.output =~ /UP-TO-DATE/)
    }

    def "format task is not up-to-date anymore, if the style option has changed"() {
        project.createFile(['Foo.java'], 'class Foo {}')
        buildfile.write("""\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  options style: 'GOOGLE'
            |}
            |""".stripMargin())

        when:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /UP-TO-DATE/

        when:
        buildfile.write("""\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  options style: 'AOSP'
            |}
            |""".stripMargin())
        result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        !(result.output =~ /UP-TO-DATE/)
    }

    def "verify task is not up-to-date when executed a second time"() {
        project.createFile(['Foo.java'], 'class Foo {}\n')

        when:
        def result = runner.withArguments(DEFAULT_VERIFY_TASK_NAME).build()

        then:
        !(result.output =~ /UP-TO-DATE/)

        when:
        result = runner.withArguments(DEFAULT_VERIFY_TASK_NAME).build()

        then:
        !(result.output =~ /UP-TO-DATE/)
    }

    def "format task doesn't modify a file, if it is already formatted correctly"() {
        def foo = project.createFile(['Foo.java'], 'class Foo {}\n')

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME, '--info').build()

        then:
        result.output =~ /Foo\.java: UP-TO-DATE/
        !foo.lastModifiedTimeHasChanged()
        !foo.contentHasChanged()
    }
}
