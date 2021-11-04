package com.github.sherter.googlejavaformatgradleplugin.format

// overwrite groovy default import of java.util.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeCheckingMode

@CompileStatic
@PackageScope
class OneDotOneFactory extends AbstractFormatterFactory {

    protected static final String removeUnusedImportsClassName = 'com.google.googlejavaformat.java.RemoveUnusedImports'
    private static final String javadocOnlyImportsEnumName = removeUnusedImportsClassName + '$JavadocOnlyImports'

    // @PackageScope not available for constructors in Gradle's (v2.0) groovy version (v2.3.2)
    protected OneDotOneFactory(ClassLoader classLoader, Configuration config) {
        super(classLoader, config)
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    public Formatter create() throws ReflectiveOperationException {
        def formatter = constructFormatter()
        def reorderImports = constructReorderImportsClosure()
        def removeUnusedImports = constructRemoveUnusedImportsClosure()
        return { String source ->
            Error cause;
            try {
                def tmp = reorderImports.call(source)
                tmp = removeUnusedImports.call(tmp)
                return formatter.formatSource(tmp)
            } catch (e) {
                if ("com.google.googlejavaformat.java.FormatterException".equals(e.getClass().getCanonicalName())) {
                    String error = "Google Java Formatter error: " + e.getMessage()
                    throw new FormatterException(error, e)
                }
                cause = e; // Unknown error
            } catch (Error e) {
                cause = e; // Unknown error
            }
            String error = "An unexpected error happened: " + cause.toString()
            throw new FormatterException(error, cause)
        }
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L65">com.google.googlejavaformat.java.JavaFormatterOptions</a> */
    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Object constructJavaFormatterOptions() {
        def style = constructStyle()
        def clazz = classLoader.loadClass(javaFormatterOptionsClassName)
        def builder = clazz.builder();
        return builder.style(style).build();
    }


    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/ImportOrderer.java#L35">com.google.googlejavaformat.java.ImportOrderer</a> */
    @CompileStatic(TypeCheckingMode.SKIP)
    Closure<String> constructReorderImportsClosure() {
        def clazz = classLoader.loadClass(importOrdererClassName)
        return { String text ->
            clazz.reorderImports(text)
        }
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/RemoveUnusedImports.java#L167">com.google.googlejavaformat.java.RemoveUnusedImports</a> */
    @CompileStatic(TypeCheckingMode.SKIP)
    protected Closure<String> constructRemoveUnusedImportsClosure() {
        def clazz = classLoader.loadClass(removeUnusedImportsClassName)
        def remover = clazz.newInstance()
        def javadocOnlyImports = constructJavadocOnlyImports()
        return { String source ->
            remover.removeUnusedImports(source, javadocOnlyImports)
        }
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/RemoveUnusedImports.java#L58">com.google.googlejavaformat.java.RemoveUnusedImports$JavadocOnlyImports</a> */
    private Object constructJavadocOnlyImports() {
            def clazz = classLoader.loadClass(javadocOnlyImportsEnumName)
            return Enum.valueOf(clazz, 'KEEP')
    }
}
