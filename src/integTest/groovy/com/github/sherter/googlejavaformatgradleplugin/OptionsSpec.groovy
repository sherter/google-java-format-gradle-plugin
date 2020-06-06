package com.github.sherter.googlejavaformatgradleplugin

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME

class OptionsSpec extends AbstractIntegrationSpec {

    def 'default format task incorporates specified style'() {
        given:
        def foo = project.createFile(['Foo.java'], 'class   Foo   { void bar()  {}}')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |$extension
            |""".stripMargin())

        when:
        runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        foo.read() == expected

        where:
        extension                                      | expected
        ""                                             | "class Foo {\n  void bar() {}\n}\n" // == GOOGLE
        "googleJavaFormat { options style: 'GOOGLE' }" | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { options style: 'AOSP' }"   | "class Foo {\n    void bar() {}\n}\n"
    }

    def 'custom format task incorporates specified style'() {
        given:
        def foo = project.createFile(['Foo.java'], 'class   Foo   { void bar()  {}}')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |task myFormat(type: ${GoogleJavaFormat.name}) {
            |  source 'Foo.java'
            |}
            |$extension
            |""".stripMargin())

        when:
        runner.withArguments('myFormat').build()

        then:
        foo.read() == expected

        where:
        extension                                      | expected
        ""                                             | "class Foo {\n  void bar() {}\n}\n" // == GOOGLE
        "googleJavaFormat { options style: 'GOOGLE' }" | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { options style: 'AOSP' }"   | "class Foo {\n    void bar() {}\n}\n"
    }

    def 'default verify task incorporates specified style'() {
        given:
        def foo = project.createFile(['Foo.java'], content)
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |$extension
            |""".stripMargin())

        when:
        def result = runner.withArguments(DEFAULT_FORMAT_TASK_NAME).build()

        then:
        result.output =~ /BUILD SUCCESSFUL/

        where:
        extension                                      | content
        ""                                             | "class Foo {\n  void bar() {}\n}\n" // == GOOGLE
        "googleJavaFormat { options style: 'GOOGLE' }" | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { options style: 'AOSP' }"   | "class Foo {\n    void bar() {}\n}\n"
    }

    def 'custom verify task incorporates specified style'() {
        given:
        def foo = project.createFile(['Foo.java'], content)
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |$extension
            |task myVerify(type: ${VerifyGoogleJavaFormat.name}) {
            |  source 'Foo.java'
            |}
            |""".stripMargin())

        when:
        def result = runner.withArguments('myVerify').build()

        then:
        result.output =~ /BUILD SUCCESSFUL/

        where:
        extension                                      | content
        ""                                             | "class Foo {\n  void bar() {}\n}\n" // == GOOGLE
        "googleJavaFormat { options style: 'GOOGLE' }" | "class Foo {\n  void bar() {}\n}\n"
        "googleJavaFormat { options style: 'AOSP' }"   | "class Foo {\n    void bar() {}\n}\n"
    }
}
