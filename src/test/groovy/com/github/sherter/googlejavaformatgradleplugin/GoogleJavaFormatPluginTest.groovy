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
        project.extensions.findByType(GoogleJavaFormatPluginExtension) != null

        and: "format task exists"
        project.tasks.findByName(GoogleJavaFormatPlugin.DEFAULT_TASK_NAME) != null

        and: "google-java-format dependency exists"
        project.configurations.getByName(GoogleJavaFormatPlugin.CONFIGURATION_NAME).dependencies.find {
            it.group == GoogleJavaFormatPlugin.GOOGLEJAVAFORMAT_GROUPID
            it.name == GoogleJavaFormatPlugin.GOOGLEJAVAFORMAT_ARTIFACTID
            it.version == GoogleJavaFormatPlugin.GOOGLEJAVAFORMAT_DEFAULT_VERSION
        } != null
    }

    def "define google-java-format tool version"() {
        def project = ProjectBuilder.builder().build()
        project.with {
            apply plugin: GoogleJavaFormatPlugin
            "${GoogleJavaFormatPlugin.EXTENSION_NAME}" {
                toolVersion = expectedVersion
            }
        }

        when:
        project.evaluate()

        then:
        project.configurations.getByName(GoogleJavaFormatPlugin.CONFIGURATION_NAME).dependencies.find {
            it.group == GoogleJavaFormatPlugin.GOOGLEJAVAFORMAT_GROUPID
            it.name == GoogleJavaFormatPlugin.GOOGLEJAVAFORMAT_ARTIFACTID
            it.version == expectedVersion
        } != null

        where:
        expectedVersion << ['1.0', '0.1', '0.1-alpha']
    }
}
