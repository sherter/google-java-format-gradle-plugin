package com.github.sherter.googlejavaformatgradleplugin

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
}
