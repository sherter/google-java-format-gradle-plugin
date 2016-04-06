package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GoogleJavaFormatPluginTest extends Specification {

    def "applying the plugin to a project"() {
        given: "a new, empty gradle project"
        def project = ProjectBuilder.builder().build()

        when: "plugin is applied"
        project.apply plugin: GoogleJavaFormatPlugin

        then: "plugin extension exists"
        def extension = project.extensions.findByName(GoogleJavaFormatPlugin.EXTENSION_NAME)
        extension != null
        extension instanceof GoogleJavaFormatExtension

        and: "format task exists"
        def formatTask = project.tasks.findByName(GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME)
        formatTask != null
        formatTask instanceof GoogleJavaFormat

        and: "verify task exists"
        def verifyTask = project.tasks.findByName(GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME)
        verifyTask != null
        verifyTask instanceof VerifyGoogleJavaFormat
    }

    def 'default tasks are populated with default inputs'() {
        given:
        Project root = ProjectBuilder.builder().build()
        Project sub = ProjectBuilder.builder().withName('sub').withParent(root).build()
        Project subSub = ProjectBuilder.builder().withName('subSub').withParent(sub).build()

        sub.buildDir('foo')
        subSub.buildDir('bar')
        def rootCustomDir = new File(root.projectDir, 'a/b/c')
        def subCustomDir = new File(sub.projectDir, 'd/e/f/g')
        def subSubCustomDir = new File(subSub.projectDir, 'h/i')
        [sub.buildDir, subSub.buildDir, rootCustomDir, subCustomDir, subSubCustomDir].each { it.mkdirs() }

        def expectedInputs =
                [ tempJava(sub.projectDir),
                  tempJava(subCustomDir),
                  tempJava(subSub.projectDir),
                  tempJava(subSubCustomDir) ] as Set

        // files that must not appear as input
        tempJava(root.projectDir)
        tempJava(rootCustomDir)
        tempJava(sub.buildDir)
        tempJava(subSub.buildDir)
        tempNonJava(root.projectDir)
        tempNonJava(sub.projectDir)
        tempNonJava(subCustomDir)
        tempNonJava(subSub.projectDir)
        tempNonJava(subSubCustomDir)

        when:
        sub.apply plugin: GoogleJavaFormatPlugin

        then:
        expectedInputs.equals(sub.tasks.getByName(GoogleJavaFormatPlugin.DEFAULT_FORMAT_TASK_NAME).inputs.files.files)
        expectedInputs.equals(sub.tasks.getByName(GoogleJavaFormatPlugin.DEFAULT_VERIFY_TASK_NAME).inputs.files.files)
    }

    static File tempJava(File directory) {
        return File.createTempFile('File', '.java', directory)
    }

    static File tempNonJava(File directory) {
        return File.createTempFile('File', '.foo', directory)
    }
}
