package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

class FormatterFactoryTest extends Specification {

    def 'fail to create a formatter'() {
        given:
        Project project = ProjectBuilder.builder().build()
        FormatterFactory factory = new FormatterFactory(project, Mock(Logger))

        when:
        factory.create(null)

        then:
        thrown NullPointerException

        when:
        factory.create('this-version-is-not-in-any-repository')

        then:
        thrown ResolveException

        when: 'a version is used that was actually released'
        factory.create('0.1-alpha')

        then: 'resolution fails nevertheless since no repository was defined'
        thrown ResolveException
    }


    // install all versions of google-java-format to local maven repository in order to speed the tests:
    //
    // mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:get \
    //   -DrepoUrl=https://jcenter.bintray.com \
    //   -Dartifact=com.google.googlejavaformat:google-java-format:0.1-alpha

    @Unroll
    def 'create and use formatter (v#version)'() {
        given:
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            jcenter()
        }
        FormatterFactory factory = new FormatterFactory(project, Mock(Logger))

        when:
        def formatter = factory.create(version)

        then:
        formatter != null

        when:
        def result = formatter.format('')

        then:
        result == '\n'

        when:
        formatter.format('x')

        then:
        FormatterException e = thrown()
        e.diagnostics().first().line() == 1
        e.diagnostics().first().column() == 0
        e.diagnostics().first().message() != null

        where:
        version << GoogleJavaFormatPlugin.GOOGLEJAVAFORMAT_VERSIONS
    }

    def 'log tool version support'() {
        given:
        Project project = ProjectBuilder.builder().build()
        project.repositories {
            mavenLocal()
            maven {
                url 'https://oss.sonatype.org/content/repositories/snapshots/'
            }
            jcenter()
        }
        Logger logger = Mock()
        def factory = new FormatterFactory(project, logger)

        when:
        factory.create(GoogleJavaFormatPlugin.GOOGLEJAVAFORMAT_VERSIONS.first())

        then:
        0 * logger._

        when:
        factory.create(unsupportedVersion)

        then:
        1 * logger.warn(*_)

        where:
        unsupportedVersion = '0.1-SNAPSHOT'
    }
}
