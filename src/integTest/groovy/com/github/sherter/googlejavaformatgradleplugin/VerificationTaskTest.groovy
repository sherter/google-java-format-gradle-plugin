package com.github.sherter.googlejavaformatgradleplugin

import spock.lang.Shared

class VerificationTaskTest extends AbstractIntegrationTest {

    @Shared String customTaskName = 'verifyCustom'

    @Override
    void customSetup() {
        buildFile << """
        apply plugin: 'com.github.sherter.google-java-format'
        """
        runner.withArguments(customTaskName)
    }

    def 'no inputs results in UP-TO-DATE task'() {
        given:
        buildFile << """
        task $customTaskName(type: ${VerifyGoogleJavaFormat.name})
        """

        when:
        def result = runner.build()

        then:
        result.output.contains(":$customTaskName UP-TO-DATE")
    }

    def 'dependency resolution failure'() {
        given:
        buildFile << """
        task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            source '${buildFile.name}'
        }
        """

        when:
        def result = runner.buildAndFail()

        then:
        result.output.contains("Could not resolve all dependencies for configuration ':googleJavaFormat")
    }

    def 'no reports for correctly formatted input source file'() {
        given:
        File sourceFile = new File(projectDir, 'File.java')
        sourceFile << 'class HelloWorld {}\n'
        buildFile << """
        repositories {
            mavenLocal()
            jcenter()
        }
        task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            source '${sourceFile.name}'
        }
        """

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
        buildFile << """
        repositories {
            mavenLocal()
            jcenter()
        }
        task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            source '${sourceFile.name}'
        }
        """

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
        buildFile << """
        repositories {
            mavenLocal()
            jcenter()
        }
        task $customTaskName(type: ${VerifyGoogleJavaFormat.name}) {
            source '${sourceFile.name}'
            ignoreFailures true
        }
        """

        when:
        def result = runner.build()

        then:
        result.output.contains("${sourceFile.path}")
    }

    def 'check task (if present) depends on default verification task'() {
        when:
        def result = runner.withArguments('tasks').build()

        then:
        result.output.readLines().find { s -> s.matches(~/^verifyGoogleJavaFormat$/) }
        !result.output.contains('check')

        when: 'after base plugin'
        buildFile.text = buildScriptBlock
        buildFile << '''\
            apply plugin: JavaBasePlugin
            apply plugin: 'com.github.sherter.google-java-format'
            '''.stripIndent()
        result = runner.withArguments('tasks', '--all').build()

        then:
        result.output.contains('check - Runs all checks.\n    verifyGoogleJavaFormat')

        when: 'before base plugin'
        buildFile.text = buildScriptBlock
        buildFile << '''\
            apply plugin: 'com.github.sherter.google-java-format'
            apply plugin: JavaBasePlugin
            '''.stripIndent()
        result = runner.withArguments('tasks', '--all').build()

        then:
        result.output.contains('check - Runs all checks.\n    verifyGoogleJavaFormat')
    }

}
