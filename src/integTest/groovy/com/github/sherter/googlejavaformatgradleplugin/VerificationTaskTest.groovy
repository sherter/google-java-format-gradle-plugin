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
        result.output.contains(":$customTaskName\n\nBUILD SUCCESSFUL")
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

}
