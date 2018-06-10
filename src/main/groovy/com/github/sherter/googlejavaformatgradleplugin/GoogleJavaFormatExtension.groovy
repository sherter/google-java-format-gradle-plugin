package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption
import com.github.sherter.googlejavaformatgradleplugin.format.Gjf
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet

@CompileStatic
class GoogleJavaFormatExtension {

    static final String DEFAULT_TOOL_VERSION = Gjf.SUPPORTED_VERSIONS.last()

    private final Project project
    private String toolVersion = null
    private Map<String, Object> options = null
    private final List<Object> source = new ArrayList<Object>();
    private final PatternFilterable patternSet = new PatternSet();

    GoogleJavaFormatExtension(Project project) {
        this.project = project
        source(defaultInputs())
    }

    private FileTree defaultInputs() {
        ConfigurableFileTree javaFiles = project.fileTree(dir: project.projectDir, includes: ['**/*.java'])
        project.allprojects { Project p ->
            javaFiles.exclude p.buildDir.path.substring(project.projectDir.path.length() + 1)
        }
        return javaFiles
    }


    void setToolVersion(String version) {
        if (options != null) {
            throw new ConfigurationException('toolVersion must be set before configuring options ' +
                    '(available options depend on the version)')
        }
        if (toolVersion != null) {
            throw new ConfigurationException('toolVersion must not be set twice')
        }
        toolVersion = version
    }

    String getToolVersion() {
        if (toolVersion == null) {
            return DEFAULT_TOOL_VERSION
        }
        return toolVersion
    }

    void setOptions(Map options) {
        throw new ConfigurationException("Not allowed; use method 'options(Map<String,String>)' to configure options");
    }

    Map<String, Object> getOptions() {
        if (options == null) {
            return Collections.emptyMap()
        }
        return Collections.unmodifiableMap(options)
    }

    void options(Map<String, Object> newOptions) {
        if (options == null) {
            options = [:]
        }
        def existing = newOptions.keySet().find { options.containsKey(it) }
        if (existing != null) {
            throw new ConfigurationException("Option '$existing' was set twice")
        }
        for (Map.Entry entry : newOptions.entrySet()) {
            if(!FormatterFactory.optionMapping.containsRow(entry.key)) {
                throw new ConfigurationException("Unsupported option '${entry.key}'")
            }
            FormatterOption option = (FormatterOption) FormatterFactory.optionMapping.get(entry.key, entry.value)
            if (option == null) {
                throw new ConfigurationException("Unsupported value '${entry.value}' for option '${entry.key}'")
            }
        }
        options.putAll(newOptions)
    }

    FileTree getSource() {
        ArrayList<Object> copy = new ArrayList<Object>(this.source);
        FileTree src = project.files(copy).getAsFileTree();
        return src == null ? project.files().getAsFileTree() : src.matching(patternSet);
    }

    void setSource(Object source) {
        this.source.clear();
        this.source.add(source);
    }

    GoogleJavaFormatExtension source(Object... sources) {
        for (Object source : sources) {
            this.source.add(source);
        }
        return this;
    }

    GoogleJavaFormatExtension include(String... includes) {
        patternSet.include(includes);
        return this;
    }

    GoogleJavaFormatExtension include(Iterable<String> includes) {
        patternSet.include(includes);
        return this;
    }

    GoogleJavaFormatExtension include(Spec<FileTreeElement> includeSpec) {
        patternSet.include(includeSpec);
        return this;
    }

    GoogleJavaFormatExtension include(Closure includeSpec) {
        patternSet.include(includeSpec);
        return this;
    }

    GoogleJavaFormatExtension exclude(String... excludes) {
        patternSet.exclude(excludes);
        return this;
    }

    GoogleJavaFormatExtension exclude(Iterable<String> excludes) {
        patternSet.exclude(excludes);
        return this;
    }

    GoogleJavaFormatExtension exclude(Spec<FileTreeElement> excludeSpec) {
        patternSet.exclude(excludeSpec);
        return this;
    }

    GoogleJavaFormatExtension exclude(Closure excludeSpec) {
        patternSet.exclude(excludeSpec);
        return this;
    }
}
