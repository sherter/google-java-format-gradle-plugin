package com.github.sherter.googlejavaformat

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GoogleJavaFormatTask extends DefaultTask {

    def formatter

    @TaskAction
    void formatJavaSources() {
        // TODO(sherter): Should be run in parallel
        inputs.files.each { file ->
            file.write(formatter.formatSource(file.text))
        }
    }
}
