package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption
import com.github.sherter.googlejavaformatgradleplugin.format.Gjf
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableTable
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.Logger

@CompileStatic
class FormatterFactory {

    static final ImmutableTable<String, Object, FormatterOption> optionMapping =
            ImmutableTable.<String, Object, FormatterOption>builder()
                    .put('style', 'GOOGLE', FormatterOption.GOOGLE_STYLE)
                    .put('style', 'AOSP', FormatterOption.AOSP_STYLE)
                    .put('javadoc', Boolean.FALSE, FormatterOption.NO_JAVADOC_FORMATTER)
                    .put('javadoc', Boolean.TRUE, FormatterOption.ECLIPSE_JAVADOC_FORMATTER)
                    .build()


    private final Project project
    private final Logger logger

    FormatterFactory(Project project, Logger logger) {
        this.project = Objects.requireNonNull(project)
        this.logger = Objects.requireNonNull(logger)
    }

    Formatter create(String toolVersion, ImmutableSet<FormatterOption> options) throws ResolveException {
        Objects.requireNonNull(toolVersion)
        def configuration = setupConfiguration(toolVersion)
        def classpath = configuration.resolve()
        def classLoader = new URLClassLoader(classpath.collect { it.toURI().toURL() } as URL[], (ClassLoader)null)
        boolean versionIsSupported = toolVersion in Gjf.SUPPORTED_VERSIONS
        if (!versionIsSupported) {
            logger.warn('Version {} of google-java-format-gradle-plugin is not tested against version {} of ' +
                    'google-java-format. This should not be a problem if the task is executed without failures.',
                    GoogleJavaFormatPlugin.PLUGIN_VERSION, toolVersion)
        }
        return Gjf.newFormatter(classLoader, toolVersion, options as FormatterOption[])
    }

    @PackageScope
    static ImmutableSet<FormatterOption> mapOptions(Map<String, Object> optionsInDsl) {
        Set<FormatterOption> mapped = new HashSet<>(optionsInDsl.size())
        for (Map.Entry<String, Object> entry : optionsInDsl) {
            def option = optionMapping.get(entry.key, entry.value)
            if (option == null) {
                throw new IllegalArgumentException("invalid option: $entry")
            }
            mapped.add(option)
        }
        return ImmutableSet.copyOf(mapped);
    }

    private Configuration setupConfiguration(String toolVersion) {
        def configuration = project.configurations.maybeCreate("googleJavaFormat$toolVersion");
        def dependency = project.dependencies.create(
                group: Gjf.GROUP_ID,
                name: Gjf.ARTIFACT_ID,
                version: toolVersion
        )
        configuration.dependencies.add(dependency)
        return configuration
    }
}
