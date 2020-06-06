package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Unroll

@Unroll
class CmdlineSpec extends AbstractIntegrationSpec {

    def "specify include pattern on command line"() {
        given:
        project.createFile(['build.gradle'], """\
            |$applyPlugin
            |$defaultRepositories
            |$downgradeToolVersionIfLatestNotSupportedOnCurrentJvm
            |""".stripMargin())
        def foo = project.createFile(['Foo.java'], 'class  Foo  {   }')
        def bar = project.createFile(['Bar.java'], 'class  Bar  {   }')

        when: "formatting task is executed"
        runner.withArguments("googleJavaFormat", "-DgoogleJavaFormat.include=*Bar*").build()

        then: "only included files are formatted"
        !foo.contentHasChanged()
        bar.contentHasChanged()

        when:
        runner.withArguments("verifyGoogleJavaFormat").build()

        then:
        thrown(UnexpectedBuildFailure)

        when:
        runner.withArguments("verifyGoogleJavaFormat", "-DverifyGoogleJavaFormat.include=*Bar*").build()

        then:
        notThrown(Throwable)
    }
}
