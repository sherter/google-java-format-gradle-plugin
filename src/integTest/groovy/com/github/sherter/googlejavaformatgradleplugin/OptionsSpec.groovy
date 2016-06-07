package com.github.sherter.googlejavaformatgradleplugin

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME

class OptionsSpec extends AbstractIntegrationSpec {

    def 'default format task incorporates specified style'() {
        given:
        def foo = project.createFile(['Foo.java'], 'class   Foo   { void bar()  {}}')
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
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

    def 'javadoc is formatted with expected javadoc formatter'() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '$toolVersion'
            |}
            |""".stripMargin())
        def foo = project.createFile(['Foo.java'], '''\
            |/**
            | * foo
            | * bar
            | */
            |class Foo {}
            |'''.stripMargin())

        when:
        runner.withArguments('goJF').build()

        then:
        foo.read() == expected

        where:
        toolVersion | expected
        '0.1-alpha' | '/**\n * foo\n * bar\n */\nclass Foo {}\n' // no javadoc formatting
        '1.0'       | '/**\n * foo bar\n */\nclass Foo {}\n' // EclipseJavadocFormatter
    }

    def 'imports are sorted if supported by tool version'() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |googleJavaFormat {
            |  toolVersion = '$toolVersion'
            |}
            |""".stripMargin())
        def foo = project.createFile(['Foo.java'], '''\
            |import second.Foo;
            |import first.Bar;
            |
            |class Foo {}
            |'''.stripMargin())

        when:
        runner.withArguments('goJF').build()

        then:
        foo.read() == expected

        where:
        // TODO(sherter): add another row as soon as a version is released that actually supports it
        toolVersion    | expected
        '0.1-alpha'    | 'import second.Foo;\nimport first.Bar;\n\nclass Foo {}\n' // no sorting
        '1.0'          | 'import second.Foo;\nimport first.Bar;\n\nclass Foo {}\n' // no sorting (google-java-format issue #42)
    }
}
