package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.Logger

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class FormatterFactory {

    static final String GOOGLEJAVAFORMAT_GROUPID = 'com.google.googlejavaformat'
    static final String GOOGLEJAVAFORMAT_ARTIFACTID = 'google-java-format'


    private final Project project
    private final Logger logger

    FormatterFactory(Project project, Logger logger) {
        this.project = Objects.requireNonNull(project)
        this.logger = Objects.requireNonNull(logger)
    }

    Formatter create(String toolVersion) throws ResolveException {
        Objects.requireNonNull(toolVersion)
        def configuration = setupConfiguration(toolVersion)
        def classpath = configuration.resolve()
        def classLoader = new URLClassLoader(classpath.collect { it.toURI().toURL() } as URL[], (ClassLoader)null)
        boolean versionIsSupported = toolVersion in GoogleJavaFormatPlugin.GOOGLEJAVAFORMAT_VERSIONS
        if (!versionIsSupported) {
            logger.warn('Version {} of google-java-format-gradle-plugin is not tested against version {} of ' +
                    'google-java-format. This should not be a problem if the task is executed without failures.',
                    GoogleJavaFormatPlugin.PLUGIN_VERSION, toolVersion)
        }
        return defaultFormatter(classLoader)
    }

    private Configuration setupConfiguration(String toolVersion) {
        def configuration = project.configurations.maybeCreate("googleJavaFormat$toolVersion");
        def dependency = project.dependencies.create(
                group: GOOGLEJAVAFORMAT_GROUPID,
                name: GOOGLEJAVAFORMAT_ARTIFACTID,
                version: toolVersion
        )
        configuration.dependencies.add(dependency)
        return configuration
    }

    @TypeChecked(SKIP)
    private Formatter defaultFormatter(ClassLoader classLoader) {
        def formatter = classLoader.loadClass('com.google.googlejavaformat.java.Formatter').newInstance()
        return { String source ->
            try {
                return formatter.formatSource(source)
            } catch (e) {
                Collection<FormatterDiagnostic> diagnostics = e.diagnostics().collect { diagnostic ->
                    FormatterDiagnostic.create(diagnostic.@lineNumber, diagnostic.@column, diagnostic.@message)
                }
                throw FormatterException.create(diagnostics)
            }
        }
    }
}
