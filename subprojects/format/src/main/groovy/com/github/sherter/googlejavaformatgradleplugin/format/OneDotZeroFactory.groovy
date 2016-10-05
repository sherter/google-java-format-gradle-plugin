package com.github.sherter.googlejavaformatgradleplugin.format

// overwrite groovy default import of java.util.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeCheckingMode

@CompileStatic
@PackageScope
final class OneDotZeroFactory implements FormatterFactory {

    private static final String formatterClassName = 'com.google.googlejavaformat.java.Formatter'
    private static final String javaFormatterOptionsClassName = 'com.google.googlejavaformat.java.JavaFormatterOptions'
    private static final String javadocFormatterEnumName = 'com.google.googlejavaformat.java.JavaFormatterOptions$JavadocFormatter'
    private static final String styleEnumName = 'com.google.googlejavaformat.java.JavaFormatterOptions$Style'
    private static final String sortImportsEnumName = 'com.google.googlejavaformat.java.JavaFormatterOptions$SortImports'
    private static final String importOrdererClassName = 'com.google.googlejavaformat.java.ImportOrderer'

    private final ClassLoader classLoader
    private final Configuration config

    // @PackageScope not available for constructors in Gradle's (v2.0) groovy version (v2.3.2)
    protected OneDotZeroFactory(ClassLoader classLoader, Configuration config) {
        this.classLoader = classLoader
        this.config = config
    }

    // to be removed from interface after transition to version specific formatter factories
    @Override
    Formatter create(FormatterOption[] options) {
        return null
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    public Formatter create() throws ReflectiveOperationException {
        def formatter = constructFormatter()
        def reorderImports = constructReorderImportsClosure()
        return { String source ->
            try {
                def importOrderedSource = reorderImports.call(source)
                return formatter.formatSource(importOrderedSource)
            } catch (e) {
                throw new FormatterException()
            }
        }
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/Formatter.java#L92">com.google.googlejavaformat.java.Formatter</a> */
    private Object constructFormatter() {
        def options = constructJavaFormatterOptions()
        def clazz = classLoader.loadClass(formatterClassName)
        return clazz.newInstance(options)
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L90">com.google.googlejavaformat.java.JavaFormatterOptions</a> */
    private Object constructJavaFormatterOptions() {
        def javadocFormatter = constructJavadocFormatter()
        def style = constructStyle()
        def sortImports = constructSortImports()
        def clazz = classLoader.loadClass(javaFormatterOptionsClassName)
        return clazz.newInstance(javadocFormatter, style, sortImports)
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L69">com.google.googlejavaformat.java.JavaFormatterOptions$JavadocFormatter</a> */
    private Object constructJavadocFormatter() {
        def clazz = classLoader.loadClass(javadocFormatterEnumName)
        return Enum.valueOf(clazz, 'ECLIPSE')
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L50">com.google.googlejavaformat.java.JavaFormatterOptions$Style</a> */
    private Object constructStyle() {
        def clazz = classLoader.loadClass(styleEnumName)
        switch (config.style) {
            case Style.GOOGLE:
                return Enum.valueOf(clazz, 'GOOGLE')
            case Style.AOSP:
                return Enum.valueOf(clazz, 'AOSP')
            default:
                // if we end up here: shame on the person who added the unknown style and didn't update all the cases!
                throw new AssertionError()
        }
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L44">com.google.googlejavaformat.java.JavaFormatterOptions$SortImports</a> */
    private Object constructSortImports() {
        def clazz = classLoader.loadClass(sortImportsEnumName)

        // It doesn't matter which value we return here, it has no influence whatsoever.
        // See https://github.com/google/google-java-format/issues/42
        return Enum.valueOf(clazz, 'NO')
    }


    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/ImportOrderer.java#L38">com.google.googlejavaformat.java.ImportOrderer</a> */
    @CompileStatic(TypeCheckingMode.SKIP)
    Closure<String> constructReorderImportsClosure() {
        def clazz = classLoader.loadClass(importOrdererClassName)
        return { String text ->
            clazz.reorderImports('', text)
        }
    }
}
