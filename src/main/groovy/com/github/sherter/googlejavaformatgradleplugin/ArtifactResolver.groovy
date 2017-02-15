package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.Gjf
import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * Downloads google-java-format and its transitive dependencies (if not already in local cache) and
 * returns a {@link ClassLoader} that can be used to load classes from google-java-format.
 */
@CompileStatic
class ArtifactResolver {
    private final Project project

    ArtifactResolver(Project project) {
        this.project = project
    }

    ClassLoader resolve(String version) {
        def conf = project.configurations.maybeCreate("googleJavaFormat$version")
        def dep = project.dependencies.create("${Gjf.GROUP_ID}:${Gjf.ARTIFACT_ID}:$version")
        conf.dependencies.add(dep)
        def classpath = conf.resolve()
        return new URLClassLoader(classpath.collect { it.toURI().toURL() } as URL[], (ClassLoader)null)
    }
}
