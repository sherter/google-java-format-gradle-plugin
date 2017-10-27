package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.Formatter
import com.google.common.collect.ImmutableSet
import org.gradle.api.Project

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SharedContext {

    private final Project project
    private ExecutorService executor
    private Formatter formatter
    private FileToStateMapper mapper
    GoogleJavaFormatExtension extension

    SharedContext(Project project, GoogleJavaFormatExtension extension) {
        this.project = Objects.requireNonNull(project)
        this.extension = extension
    }

    synchronized ExecutorService executor() {
        if (executor == null) {
            // single threaded for now
            // further synchronization for FormatFileTasks and VerifyFileTasks
            // is required if we want to write to files concurrently
            executor = Executors.newSingleThreadExecutor()
        }
        return executor
    }

    synchronized FileToStateMapper mapper() {
        if (mapper == null) {
            mapper = new FileToStateMapper()
            PersistenceModule module = new PersistenceModule(project)
            PersistenceComponent component = DaggerPersistenceComponent.builder().persistenceModule(module).build()
            def optionsStore = setupOptionsStore(component.optionsStore());
            def fileInfoStore = setupFileStore(component.fileInfoStore(), mapper)
            try {
                def options = optionsStore.read()
                def extension = project.extensions.getByName(GoogleJavaFormatPlugin.EXTENSION_NAME)
                if ((options.version() != extension.toolVersion) ||
                        (!options.options().equals(FormatterFactory.mapOptions(extension.getOptions())))) {
                    project.logger.info("Formatter options changed; invalidating saved file states")
                    fileInfoStore.clear()
                } else {
                    ImmutableSet<FileInfo> states = ImmutableSet.of()
                    try {
                        states = fileInfoStore.read()
                    } catch (IOException e) {
                        project.getLogger().error("Failed to load formatting states from disk", e)
                    }
                    for(FileInfo info : states) {
                        mapper.putIfNewer(info)
                    }
                }
            } catch (IOException e) {

            }
        }
        return mapper
    }

    FormatterOptionsStore setupOptionsStore(FormatterOptionsStore optionsStore) {
         project.gradle.buildFinished {
            def extension = project.extensions.getByName(GoogleJavaFormatPlugin.EXTENSION_NAME)
            def options = FormatterOptions.create(extension.toolVersion,
                    FormatterFactory.mapOptions(extension.getOptions()))
            try {
                optionsStore.write(options);
            } finally {
                optionsStore.close();
            }
        }
        return optionsStore
    }

    private FileInfoStore setupFileStore(FileInfoStore store, FileToStateMapper mapper) {
        project.gradle.buildFinished {
            try {
                store.update(mapper)
            } catch (IOException e) {
                project.getLogger().error("Failed to write formatting states to disk", e)
            } finally {
                store.close()
            }
        }
        return store
    }

    synchronized Formatter formatter() {
        if (formatter == null) {
            GoogleJavaFormatExtension extension = (GoogleJavaFormatExtension) project.getExtensions().getByName(GoogleJavaFormatPlugin.getEXTENSION_NAME())
            formatter = new FormatterFactory(project, project.logger).create(extension.toolVersion,
                    FormatterFactory.mapOptions(extension.getOptions()), extension.sortImports)
        }
        return formatter
    }
}
