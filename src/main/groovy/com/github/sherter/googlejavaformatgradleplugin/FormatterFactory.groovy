package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.Gjf
import com.github.sherter.googlejavaformatgradleplugin.format.Style
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableTable
import com.google.common.collect.Iterables
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.Logger

@CompileStatic
class FormatterFactory {

    static final ImmutableTable<String, Object, Style> optionMapping =
            ImmutableTable.<String, Object, Style>builder()
                    .put('style', 'GOOGLE', Style.GOOGLE)
                    .put('style', 'AOSP', Style.AOSP)
                    .build()


    private final Project project
    private final Logger logger

    FormatterFactory(Project project, Logger logger) {
        this.project = Objects.requireNonNull(project)
        this.logger = Objects.requireNonNull(logger)
    }

    Formatter create(String toolVersion, Style style) {
        def classLoader = new ArtifactResolver(project).resolve(toolVersion)
        boolean versionIsSupported = toolVersion in Gjf.SUPPORTED_VERSIONS
        if (!versionIsSupported) {
            logger.warn('Version {} of google-java-format-gradle-plugin is not tested against version {} of ' +
                    'google-java-format. This should not be a problem if the task is executed without failures.',
                    GoogleJavaFormatPlugin.PLUGIN_VERSION, toolVersion)
        }
        return Gjf.newFormatter(classLoader, new com.github.sherter.googlejavaformatgradleplugin.format.Configuration(
                    toolVersion, style))
    }

    @PackageScope
    static Style mapOptions(Map<String, Object> optionsInDsl) {
        Set<Style> styles = new HashSet<Style>(optionsInDsl.size())
        for (Map.Entry<String, Object> entry : optionsInDsl) {
            def option = (Style) optionMapping.get(entry.key, entry.value)
            if (option == null) {
                throw new IllegalArgumentException("invalid option: $entry")
            }
            styles.add(option)
        }
        return styles.isEmpty() ? Style.GOOGLE : Iterables.getOnlyElement(styles)
    }
}
