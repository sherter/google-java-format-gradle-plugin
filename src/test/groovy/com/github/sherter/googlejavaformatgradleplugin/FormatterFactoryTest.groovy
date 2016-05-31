package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.FormatterException
import com.github.sherter.googlejavaformatgradleplugin.format.Gjf
import com.google.common.collect.ImmutableSet
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
        factory.create(null, ImmutableSet.of())

        then:
        thrown NullPointerException

        when:
        factory.create('this-version-is-not-in-any-repository', ImmutableSet.of())

        then:
        thrown ResolveException

        when: 'a version is used that was actually released'
        factory.create('0.1-alpha', ImmutableSet.of())

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
        def formatter = factory.create(version, ImmutableSet.of())

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

        where:
        version << Gjf.SUPPORTED_VERSIONS
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
        factory.create(Gjf.SUPPORTED_VERSIONS.first(), ImmutableSet.of())

        then:
        0 * logger._

        when:
        factory.create(unsupportedVersion, ImmutableSet.of())

        then:
        1 * logger.warn(*_)

        where:
        unsupportedVersion = '0.1-SNAPSHOT'
    }
}
