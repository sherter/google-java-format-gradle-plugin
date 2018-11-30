package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption
import com.github.sherter.googlejavaformatgradleplugin.format.Gjf
import com.github.sherter.googlejavaformatgradleplugin.format.Style
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableTable
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.Logger

class FormatterFactory {

    static final ImmutableTable<String, Object, FormatterOption> optionMapping =
            ImmutableTable.<String, Object, FormatterOption>builder()
                    .put('style', 'GOOGLE', FormatterOption.GOOGLE_STYLE)
                    .put('style', 'AOSP', FormatterOption.AOSP_STYLE)
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
        def classLoader = new URLClassLoader(classpath.collect { it.toURI().toURL() } as URL[], ClassLoader.getSystemClassLoader())
        boolean versionIsSupported = toolVersion in Gjf.SUPPORTED_VERSIONS
        if (!versionIsSupported) {
            logger.info('Version {} of google-java-format-gradle-plugin is not tested against version {} of ' +
                    'google-java-format. This should not be a problem if the task is executed without failures.',
                    GoogleJavaFormatPlugin.PLUGIN_VERSION, toolVersion)
        }

        if (options.contains(FormatterOption.AOSP_STYLE)) {
            return Gjf.newFormatter(classLoader, new com.github.sherter.googlejavaformatgradleplugin.format.Configuration(
                    toolVersion, Style.AOSP))
        } else {
            return Gjf.newFormatter(classLoader, new com.github.sherter.googlejavaformatgradleplugin.format.Configuration(
                    toolVersion, Style.GOOGLE))
        }
    }

    @PackageScope
    static ImmutableSet<FormatterOption> mapOptions(Map<String, Object> optionsInDsl) {
        Set<FormatterOption> mapped = new HashSet<FormatterOption>(optionsInDsl.size())
        for (Map.Entry<String, Object> entry : optionsInDsl) {
            def option = (FormatterOption) optionMapping.get(entry.key, entry.value)
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
