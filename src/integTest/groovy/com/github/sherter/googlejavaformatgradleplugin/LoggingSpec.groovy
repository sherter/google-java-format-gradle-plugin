package com.github.sherter.googlejavaformatgradleplugin

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME

class LoggingSpec extends AbstractIntegrationSpec {

    def "format task logs previously unformatted files as now formatted"() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |""".stripMargin())
        project.createFile(['src', 'main', 'java', 'Foo.java'], 'class    Foo  {  }')
        project.createFile(['Bar.java'], 'class Bar {}\n')

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /Foo\.java: formatted successfully/
        !(result.output =~ /Bar\.java: formatted successfully/)
    }

    def "format task logs already formatted files as UP-TO-DATE"() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |""".stripMargin())
        project.createFile(['src', 'main', 'java', 'Foo.java'], 'class    Foo  {  }')
        project.createFile(['Bar.java'], 'class Bar {}\n')

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME, '--info').build()

        then:
        !(result.output =~ /Foo\.java: UP-TO-DATE/)
        result.output =~ /Bar\.java: UP-TO-DATE/

        when:
        result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME, '--info').build()

        then:
        result.output =~ /Foo\.java: UP-TO-DATE/
        result.output =~ /Bar\.java: UP-TO-DATE/
    }

    def "format task logs non-Java files in its inputs and makes the build fail"() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |""".stripMargin())
        project.createFile(['src', 'main', 'java', 'Foo.java'], 'class    Foo  {  }')
        project.createFile(['Baz.java'], 'this is not Java')
        project.createFile(['Bar.java'], 'class Bar {}\n')
        project.createFile(['OtherNon.java'], 'is not Java')

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME, '--info').buildAndFail()

        then:
        result.output =~ /Foo\.java: formatted successfully/
        result.output =~ /Bar\.java: UP-TO-DATE/
        // (?s) makes the regex match newlines with . (dot) operators
        result.output =~ /(?s)Failed to format the following files.*Baz\.java/
        result.output =~ /(?s)Failed to format the following files.*OtherNon\.java/
    }
}
