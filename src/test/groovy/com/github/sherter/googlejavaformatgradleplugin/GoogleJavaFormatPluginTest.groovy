package com.github.sherter.googlejavaformatgradleplugin

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
        project.tasks.findByName(GoogleJavaFormatPlugin.DEFAULT_TASK_NAME) != null
    }
}
