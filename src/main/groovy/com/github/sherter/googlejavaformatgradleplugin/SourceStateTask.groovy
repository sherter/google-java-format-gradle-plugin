package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.tasks.SourceTask

class SourceStateTask extends SourceTask {
    protected FileStateHandler fileStateHandler

    void setFileStateHandler(FileStateHandler fsh) {
        this.fileStateHandler = fsh
    }

    FileStateHandler getFileStateHandler() {
        return this.fileStateHandler
    }
}
