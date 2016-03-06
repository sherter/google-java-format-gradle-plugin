package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GoogleJavaFormatPluginTest extends Specification {

    def "applying the plugin to a project"() {
        given: "a new, empty gradle project"
        def project = ProjectBuilder.builder().build()

        when: "plugin is applied and project is evaluated"
        project.apply plugin: GoogleJavaFormatPlugin
        project.evaluate()

        then: "plugin extension exists"
        project.extensions.findByType(GoogleJavaFormatExtension) != null

        and: "format task exists"
        def formatTask = project.tasks.findByName(GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME)
        formatTask != null
        formatTask instanceof GoogleJavaFormat

        and: "verify task exists"
        def verifyTask = project.tasks.findByName(GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME)
        verifyTask != null
        verifyTask instanceof VerifyGoogleJavaFormat
    }

    def "predefined tasks' default inputs"() {
        given:
        Project root = ProjectBuilder.builder().build()
        Project sub = ProjectBuilder.builder().withName('sub').withParent(root).build()
        Project subSub = ProjectBuilder.builder().withName('subSub').withParent(sub).build()

        sub.plugins.apply(GoogleJavaFormatPlugin)

        sub.buildDir('foo')
        subSub.buildDir('bar')
        File javaFile = new File('Baz.java')

        File inRoot = root.file(javaFile)
        File inSub = sub.file(javaFile)
        File inSubBuild = new File(sub.buildDir, javaFile.name)
        File inSubSub = subSub.file(javaFile)
        File inSubSubBuild = new File(subSub.buildDir, javaFile.name)
        [sub.buildDir, subSub.buildDir].each { it.mkdirs() }
        [inRoot, inSub, inSubBuild, inSubSub, inSubSubBuild].each { it.createNewFile() }

        when:
        sub.evaluate()

        then:
        def formatInputs = sub.tasks.withType(GoogleJavaFormat).first().inputs.files.files
        def verifyInputs = sub.tasks.withType(VerifyGoogleJavaFormat).first().inputs.files.files
        [inRoot, inSubBuild, inSubSubBuild].each {
            assert !formatInputs.contains(it)
            assert !verifyInputs.contains(it)
        }
        [inSub, inSubSub].each {
            assert formatInputs.contains(it)
            assert verifyInputs.contains(it)
        }
    }
}
