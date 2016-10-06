package com.github.sherter.googlejavaformatgradleplugin.format

// overwrite groovy default import of java.util.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeCheckingMode

@CompileStatic
@PackageScope
final class OneDotZeroFactory extends AbstractFormatterFactory {

    private static final String javadocFormatterEnumName = 'com.google.googlejavaformat.java.JavaFormatterOptions$JavadocFormatter'
    private static final String sortImportsEnumName = 'com.google.googlejavaformat.java.JavaFormatterOptions$SortImports'

    // @PackageScope not available for constructors in Gradle's (v2.0) groovy version (v2.3.2)
    protected OneDotZeroFactory(ClassLoader classLoader, Configuration config) {
        super(classLoader, config)
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

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L90">com.google.googlejavaformat.java.JavaFormatterOptions</a> */
    @Override
    Object constructJavaFormatterOptions() {
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
