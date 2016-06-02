package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Shared

class VerificationTaskTest extends AbstractIntegrationTest {

    @Shared String customTaskName = 'verifyCustom'

    @Override
    void customSetup() {
        runner.withArguments(customTaskName)
    }

    def 'no inputs results in UP-TO-DATE task'() {
        given:
        buildFile.text = """\
            |$applyPlugin
            |
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name})
            |""".stripMargin()

        when:
        def result = runner.build()

        then:
        result.output.contains(":$customTaskName UP-TO-DATE")
    }

    def 'dependency resolution failure'() {
        given:
        buildFile.text = """\
            |$applyPlugin
            |
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            |  source '${buildFile.name}'
            |}
            |""".stripMargin()

        when:
        def result = runner.buildAndFail()

        then:
        result.output.contains("Could not resolve all dependencies for configuration ':googleJavaFormat")
    }

    def 'no reports for correctly formatted input source file'() {
        given:
        File sourceFile = new File(projectDir, 'File.java')
        sourceFile << 'class HelloWorld {}\n'
        buildFile.text = """\
            |$applyPlugin
            |
            |repositories {
            |  mavenLocal()
            |  jcenter()
            |}
            |
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            |  source '${sourceFile.name}'
            |}
            |""".stripMargin()

        when:
        def result = runner.build()

        then:
        result.output.contains(":$customTaskName\n")
        result.output.contains('BUILD SUCCESSFUL\n')
    }

    def 'report badly formatted input source file'() {
        given:
        File sourceFile = new File(projectDir, 'File.java')
        sourceFile << """
        class       HelloWorld {
                }
        """
        buildFile.text = """\
            |$applyPlugin
            |
            |repositories {
            |  mavenLocal()
            |  jcenter()
            |}
            |
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            |  source '${sourceFile.name}'
            |}
            |""".stripMargin()

        when:
        def result = runner.buildAndFail()

        then:
        result.output.contains("${sourceFile.path}")
    }

    def 'ignore verification failures'() {
        given:
        File sourceFile = new File(projectDir, 'File.java')
        sourceFile << """
        class       HelloWorld {
                }
        """
        buildFile.text = """\
            |$applyPlugin
            |
            |repositories {
            |  mavenLocal()
            |  jcenter()
            |}
            |
            |task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            |  source '${sourceFile.name}'
            |  ignoreFailures true
            |}
            |""".stripMargin()

        when:
        def result = runner.build()

        then:
        result.output.contains("${sourceFile.path}")
    }

    def 'check task (if present) depends on default verification task'() {
        when:
        buildFile.text = applyPlugin
        def result = runner.withArguments('tasks').build()

        then:
        result.output.readLines().find { s -> s.matches(~/^verifyGoogleJavaFormat$/) }
        !result.output.contains('check')

        when: 'after base plugin'
        buildFile.text = """\
            |$buildScriptBlock
            |apply plugin: JavaBasePlugin
            |apply plugin: 'com.github.sherter.google-java-format'
            |""".stripMargin()
        result = runner.withArguments('tasks', '--all').build()

        then:
        result.output.contains('check - Runs all checks.\n    verifyGoogleJavaFormat')

        when: 'before base plugin'
        buildFile.text = """\
            |$buildScriptBlock
            |apply plugin: 'com.github.sherter.google-java-format'
            |apply plugin: JavaBasePlugin
            |""".stripMargin()
        result = runner.withArguments('tasks', '--all').build()

        then:
        result.output.contains('check - Runs all checks.\n    verifyGoogleJavaFormat')
    }

}
