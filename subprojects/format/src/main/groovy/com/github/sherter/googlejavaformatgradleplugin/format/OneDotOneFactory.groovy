package com.github.sherter.googlejavaformatgradleplugin.format

// overwrite groovy default import of java.util.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeCheckingMode

@CompileStatic
@PackageScope
final class OneDotOneFactory extends AbstractFormatterFactory {

    private static final String removeUnusedImportsClassName = 'com.google.googlejavaformat.java.RemoveUnusedImports'
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
            try {
                def tmp = reorderImports.call(source)
                tmp = removeUnusedImports.call(tmp)
                return formatter.formatSource(tmp)
            } catch (e) {
                throw new FormatterException()
            } catch (Error e) {
                throw new FormatterException()
            }
        }
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L65">com.google.googlejavaformat.java.JavaFormatterOptions</a> */
    @Override
    Object constructJavaFormatterOptions() {
        def style = constructStyle()
        def clazz = classLoader.loadClass(javaFormatterOptionsClassName)
        return clazz.newInstance(style)
    }


    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/ImportOrderer.java#L35">com.google.googlejavaformat.java.ImportOrderer</a> */
    @CompileStatic(TypeCheckingMode.SKIP)
    Closure<String> constructReorderImportsClosure() {
        def clazz = classLoader.loadClass(importOrdererClassName)
        return { String text ->
            if (config.sortImports) {
              clazz.reorderImports(text)
            } else {
              text
            }
        }
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/RemoveUnusedImports.java#L167">com.google.googlejavaformat.java.RemoveUnusedImports</a> */
    @CompileStatic(TypeCheckingMode.SKIP)
    private Closure<String> constructRemoveUnusedImportsClosure() {
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
