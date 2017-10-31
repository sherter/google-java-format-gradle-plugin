package com.github.sherter.googlejavaformatgradleplugin

import static com.github.sherter.googlejavaformatgradleplugin.GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME

class SortImportsSpec extends AbstractIntegrationSpec {
    def 'orderImports is honored'() {
        given:
        def foo = project.createFile(['Foo.java'], '\nimport java.util.List;\nimport java.util.ArrayList;\n\nclass Foo {\n  List<String> a = new ArrayList<>();\n}\n')
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
        extension                                  | expected
        ""                                         | "\nimport java.util.ArrayList;\nimport java.util.List;\n\nclass Foo {\n  List<String> a = new ArrayList<>();\n}\n"
        "googleJavaFormat { orderImports = true }"  | "\nimport java.util.ArrayList;\nimport java.util.List;\n\nclass Foo {\n  List<String> a = new ArrayList<>();\n}\n"
        "googleJavaFormat { orderImports = false }" | "\nimport java.util.List;\nimport java.util.ArrayList;\n\nclass Foo {\n  List<String> a = new ArrayList<>();\n}\n"
    }
}
