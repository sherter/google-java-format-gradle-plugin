package com.github.sherter.googlejavaformatgradleplugin

import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.SourceTask

class SourceStateTask extends SourceTask implements ConfigurableTask {
    protected FileStateHandler fileStateHandler

    void setFileStateHandler(FileStateHandler fsh) {
        this.fileStateHandler = fsh
    }

    FileStateHandler getFileStateHandler() {
        return this.fileStateHandler
    }

    @Override
    void configure(SharedContext context) {
        setFileStateHandler(context.fileStateHandler)
        exclude { FileTreeElement f -> fileStateHandler.isUpToDate(f.file) }
    }
}
