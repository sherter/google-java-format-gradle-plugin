package com.github.sherter.googlejavaformatgradleplugin

import com.google.common.collect.ImmutableSet
import org.gradle.api.Project

class SharedContext {

    private final Project project
    private FileStateHandler fileStateHandler
    private Formatter formatter
    private FileToStateMapper mapper
    private PersistenceComponent persist

    SharedContext(Project project) {
        this.project = Objects.requireNonNull(project)
    }

    synchronized FileStateHandler fileStateHandler() {
        if (fileStateHandler == null) {
            String buildCacheSubDir = "google-java-format/${GoogleJavaFormatPlugin.PLUGIN_VERSION}"
            GoogleJavaFormatExtension extension = project.extensions.findByType(GoogleJavaFormatExtension)
            fileStateHandler = new FileStateHandler(
                    project.projectDir,
                    new File(project.buildDir, buildCacheSubDir),
                    extension.toolVersion)
            fileStateHandler.load()
            project.gradle.buildFinished { fileStateHandler.flush() }
        }
        return fileStateHandler
    }

    synchronized FileToStateMapper mapper() {
        if (mapper == null) {
            mapper = new FileToStateMapper()
            def persistenceModule = new PersistenceModule(project)
            def store = DaggerPersistenceComponent.builder().persistenceModule(persistenceModule).build().store()
            project.gradle.buildFinished {
                try {
                    store.update(mapper)
                } catch (IOException e) {
                    project.logger.error('Failed to write formatting states to disk: {}', e.message)
                }
            }
            ImmutableSet<FileInfo> states = ImmutableSet.of()
            try {
                states = store.read()
            } catch (IOException e) {
                project.logger.error('Failed to load formatting states from disk: {}', e.message)
            }
            states.each {
                mapper.putIfNewer(it)
            }
        }
        return mapper;
    }

    synchronized Formatter formatter() {
        if (formatter == null) {
            GoogleJavaFormatExtension extension = project.extensions.findByType(GoogleJavaFormatExtension)
            formatter = new FormatterFactory(project, project.logger).create(extension.toolVersion)
        }
        return formatter
    }
}
