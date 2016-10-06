package com.github.sherter.googlejavaformatgradleplugin.format;

// overwrite groovy default import of java.util.Formatter
import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
@PackageScope
abstract class AbstractFormatterFactory implements FormatterFactory {

    static final String formatterClassName = 'com.google.googlejavaformat.java.Formatter'
    static final String javaFormatterOptionsClassName = 'com.google.googlejavaformat.java.JavaFormatterOptions'
    static final String styleEnumName = 'com.google.googlejavaformat.java.JavaFormatterOptions$Style'
    static final String importOrdererClassName = 'com.google.googlejavaformat.java.ImportOrderer'


    final ClassLoader classLoader
    final Configuration config

    AbstractFormatterFactory(ClassLoader classLoader, Configuration config) {
        this.classLoader = classLoader;
        this.config = config;
    }

    // to be removed from interface after transition to version specific formatter factories
    @Override
    public Formatter create(FormatterOption[] options) {
        return null;
    }

    /**
     * <ul>
     *     <li>v1.0: <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/Formatter.java#L92">com.google.googlejavaformat.java.Formatter</a>
     *     <li>v1.1: <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/Formatter.java#L88">com.google.googlejavaformat.java.Formatter</a>
     * </ul>
     */
    Object constructFormatter() {
        def options = constructJavaFormatterOptions()
        def clazz = classLoader.loadClass(formatterClassName)
        return clazz.newInstance(options)
    }

    abstract Object constructJavaFormatterOptions()

    /**
     * <ul>
     *     <li>v1.0: <a href="https://github.com/google/google-java-format/blob/google-java-format-1.0/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L50">com.google.googlejavaformat.java.JavaFormatterOptions$Style</a>
     *     <li>v1.1: <a href="https://github.com/google/google-java-format/blob/google-java-format-1.1/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java#L44">com.google.googlejavaformat.java.JavaFormatterOptions$Style</a>
     * </ul>
     */
    Object constructStyle() {
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
}
