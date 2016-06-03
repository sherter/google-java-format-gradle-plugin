package com.github.sherter.googlejavaformatgradleplugin

class VerificationTaskTest extends AbstractIntegrationSpec {

    final static String customTaskName = 'verifyCustom'

    def 'no inputs results in UP-TO-DATE task'() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name})
            |""".stripMargin())

        when:
        def result = runner.withArguments(customTaskName).build()

        then:
        result.output.contains(":$customTaskName UP-TO-DATE")
    }

    def 'dependency resolution failure'() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            |  source 'build.gradle'
            |}
            |""".stripMargin())

        when:
        def result = runner.withArguments(customTaskName).buildAndFail()

        then:
        result.output.contains("Could not resolve all dependencies for configuration ':googleJavaFormat")
    }

    def 'no reports for correctly formatted input source file'() {
        given:
        project.createFile(['Foo.java'], 'class Foo {}\n')
        project.createFile(['build.gradle'],  """\
            |$applyPlugin
            |$defaultRepositories
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            |  source 'Foo.java'
            |}
            |""".stripMargin())


        when:
        def result = runner.withArguments(customTaskName).build()

        then:
        result.output.contains(":$customTaskName\n")
        result.output.contains('BUILD SUCCESSFUL\n')
    }

    def 'report badly formatted input source file'() {
        given:
        project.createFile(['Foo.java'], '  class  Foo {   }')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            |  source 'Foo.java'
            |}
            |""".stripMargin())

        when:
        def result = runner.withArguments(customTaskName).buildAndFail()

        then:
        result.output.contains('Foo.java')
    }

    def 'ignore verification failures'() {
        given:
        project.createFile(['Foo.java'], '  class  Foo {   }')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories            |
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            |  source 'Foo.java'
            |  ignoreFailures true
            |}
            |""".stripMargin())

        when:
        def result = runner.withArguments(customTaskName).build()

        then:
        result.output.contains('Foo.java')
    }

    def 'check task (if present) depends on default verification task'() {
        when:
        def buildFile = project.createFile(['build.gradle'], applyPlugin)
        def result = runner.withArguments('tasks').build()

        then:
        result.output.readLines().find { s -> s.matches(~/^verifyGoogleJavaFormat$/) }
        !result.output.contains('check')

        when: 'after base plugin'
        buildFile.write("""\
            |$buildScriptBlock
            |apply plugin: JavaBasePlugin
            |apply plugin: 'com.github.sherter.google-java-format'
            |""".stripMargin())
        result = runner.withArguments('tasks', '--all').build()

        then:
        result.output.contains('check - Runs all checks.\n    verifyGoogleJavaFormat')

        when: 'before base plugin'
        buildFile.write("""\
            |$buildScriptBlock
            |apply plugin: 'com.github.sherter.google-java-format'
            |apply plugin: JavaBasePlugin
            |""".stripMargin())
        result = runner.withArguments('tasks', '--all').build()

        then:
        result.output.contains('check - Runs all checks.\n    verifyGoogleJavaFormat')
    }
}
