package com.github.sherter.googlejavaformatgradleplugin.format

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TypeCheckingMode

@CompileStatic
@PackageScope
final class OneDotEightFactory extends OneDotOneFactory {

    // @PackageScope not available for constructors in Gradle's (v2.0) groovy version (v2.3.2)
    protected OneDotEightFactory(ClassLoader classLoader, Configuration config) {
        super(classLoader, config)
    }

    /** See <a href="https://github.com/google/google-java-format/blob/google-java-format-1.8/core/src/main/java/com/google/googlejavaformat/java/RemoveUnusedImports.java#L186">com.google.googlejavaformat.java.RemoveUnusedImports</a> */
    @CompileStatic(TypeCheckingMode.SKIP)
    @Override
    Closure<String> constructRemoveUnusedImportsClosure() {
        def clazz = classLoader.loadClass(removeUnusedImportsClassName)
        def remover = clazz.newInstance()
        return { String source ->
            remover.removeUnusedImports(source)
        }
    }
}
