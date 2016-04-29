package com.github.sherter.googlejavaformatgradleplugin.format

import groovy.transform.CompileStatic

// overwrite groovy default import of java.util.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import groovy.transform.TypeCheckingMode

@CompileStatic
@PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.METHODS, PackageScopeTarget.FIELDS])
class NoOptionsFactory implements FormatterFactory {

    private final ClassLoader classLoader

    NoOptionsFactory(ClassLoader classLoader) {
        this.classLoader = classLoader
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    public Formatter create(FormatterOption[] options) {
        if (options.length > 0) {
            throw new IllegalArgumentException("Options are not supported")
        }
        def formatter = classLoader.loadClass('com.google.googlejavaformat.java.Formatter').newInstance()
        return { String source ->
            try {
                return formatter.formatSource(source)
            } catch (e) {
                throw new FormatterException()
            }
        }
    }
}
