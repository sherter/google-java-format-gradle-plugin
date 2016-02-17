package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.FormatterException.ErrorInfo
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class FormatterFactory {

    private static final Logger logger = Logging.getLogger(GoogleJavaFormatPlugin.class)

    private final Configuration configuration;
    private final String toolVersion
    private ClassLoader classLoader;
    private final Object lock = new Object();

    FormatterFactory(Configuration configuration, String toolVersion) {
        this.configuration = configuration
        this.toolVersion = toolVersion
    }

    private ClassLoader getClassLoader() {
        synchronized(lock) {
            if (classLoader == null) {
                // if not already present, resolve() will also try to download the artifacts
                Set<File> artifacts = configuration.resolve()
                List<URL> urls = artifacts.collect { it.toURI().toURL() }
                this.classLoader = new URLClassLoader(urls as URL[])
            }
        }
        return classLoader;
    }


    Formatter create() {
        def googleFormatter = getClassLoader().loadClass('com.google.googlejavaformat.java.Formatter').newInstance()
        switch (toolVersion) {
            case '0.1-alpha':
                return defaultFormatter(googleFormatter)
            default:
                logger.warn('Version "{}" of google-java-format is not officially supported. ' +
                        'If the API is the same as in version "{}" everything should be fine though.',
                        toolVersion, '0.1-alpha')
                return defaultFormatter(googleFormatter)
        }
    }

    @TypeChecked(SKIP)
    private Formatter defaultFormatter(def formatter) {
        return new Formatter() {
            @Override
            String format(String source) throws FormatterException {
                try {
                    return formatter.formatSource(source)
                } catch(e) {
                    Collection<ErrorInfo> errors = e.diagnostics().collect { diagnostic ->
                        return new ErrorInfo(
                                line: diagnostic.@lineNumber,
                                column: diagnostic.@column,
                                message: diagnostic.@message)
                    }
                    throw new FormatterException(errors)
                }
            }
        }
    }
}
