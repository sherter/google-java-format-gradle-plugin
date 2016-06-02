package com.github.sherter.googlejavaformatgradleplugin.format

// overwrite groovy default import of java.util.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import groovy.transform.TypeCheckingMode

import static com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption.*

@CompileStatic
@PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.METHODS, PackageScopeTarget.FIELDS])
class OptionsFactory implements FormatterFactory {

    private static final ImmutableMap<FormatterOption, ImmutableSet<FormatterOption>> optionConflicts =
            ImmutableMap.<FormatterOption, ImmutableSet<FormatterOption>>builder()
                    .put(AOSP_STYLE, ImmutableSet.of(GOOGLE_STYLE))
                    .put(GOOGLE_STYLE, ImmutableSet.of(AOSP_STYLE))
                    .put(NO_JAVADOC_FORMATTER, ImmutableSet.of(ECLIPSE_JAVADOC_FORMATTER))
                    .put(ECLIPSE_JAVADOC_FORMATTER, ImmutableSet.of(NO_JAVADOC_FORMATTER))
                    .put(SORT_IMPORTS, ImmutableSet.of())
                    .build()

    private final ClassLoader classLoader

    OptionsFactory(ClassLoader classLoader) {
        this.classLoader = classLoader
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    public Formatter create(FormatterOption[] options) {
        validateOptions(options)
        def formatterClass = classLoader.loadClass('com.google.googlejavaformat.java.Formatter')
        def javaFormatterOptionsClass = classLoader.loadClass('com.google.googlejavaformat.java.JavaFormatterOptions')
        def optionsObject = javaFormatterOptionsClass.newInstance(
                getJavadocFormatter(options), getStyle(options), getSortImports(options))
        def formatter = formatterClass.newInstance(optionsObject)
        return { String source ->
            try {
                return formatter.formatSource(source)
            } catch (e) {
                throw new FormatterException()
            }
        }
    }

    private static void validateOptions(FormatterOption[] options) {
        for (FormatterOption option : options) {
            if (!optionConflicts.containsKey(option)) {
                throw new IllegalArgumentException('Unsupported option: ' + option);
            }
            def conflicting = optionConflicts.get(option)
            def found = options.find { conflicting.contains(it) }
            if (found != null) {
                throw new IllegalArgumentException('Conflicting options: ' + option + ' and ' + found)
            }
        }
    }

    private getJavadocFormatter(FormatterOption[] options) {
        def javadocFormatterEnum = classLoader.loadClass('com.google.googlejavaformat.java.JavaFormatterOptions$JavadocFormatter')
        if (options.contains(ECLIPSE_JAVADOC_FORMATTER)) {
            return Enum.valueOf(javadocFormatterEnum, 'ECLIPSE')
        }
        return Enum.valueOf(javadocFormatterEnum, 'NONE')
    }

    private getStyle(FormatterOption[] options) {
        def styleEnum = classLoader.loadClass('com.google.googlejavaformat.java.JavaFormatterOptions$Style')
        if (options.contains(AOSP_STYLE)) {
            return Enum.valueOf(styleEnum, 'AOSP')
        }
        return Enum.valueOf(styleEnum, 'GOOGLE')
    }

    private getSortImports(FormatterOption[] options) {
        def sortImportsEnum = classLoader.loadClass('com.google.googlejavaformat.java.JavaFormatterOptions$SortImports')
        if (options.contains(SORT_IMPORTS)) {
            return Enum.valueOf(sortImportsEnum, 'ALSO')
        }
        return Enum.valueOf(sortImportsEnum, 'NO')
    }
}
