package com.github.sherter.googlejavaformatgradleplugin

import com.github.sherter.googlejavaformatgradleplugin.format.FormatterOption
import com.github.sherter.googlejavaformatgradleplugin.format.Gjf
import groovy.transform.CompileStatic

@CompileStatic
class GoogleJavaFormatExtension {

    static final String DEFAULT_TOOL_VERSION = '1.1'

    private String toolVersion = null
    private Map<String, Object> options = null


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
            // if toolVersion is officially supported we can do further validation and
            // check if specific options are available for that version
            if (getToolVersion() in Gjf.SUPPORTED_VERSIONS && !(getToolVersion() in option.supportedVersions)) {
                throw new ConfigurationException("Option '${entry.key}: ${entry.value}' is not supported " +
                        "by version '${getToolVersion()}' of googel-java-format")
            }
        }
        options.putAll(newOptions)
    }
}
