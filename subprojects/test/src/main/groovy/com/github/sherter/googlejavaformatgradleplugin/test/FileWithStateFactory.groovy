package com.github.sherter.googlejavaformatgradleplugin.test

import java.nio.charset.StandardCharsets

class FileWithStateFactory {

    final File rootDirectory

    FileWithStateFactory(File projectDir) {
        this.rootDirectory = projectDir
    }

    FileWithState file(Iterable<String> pathElements, String content) {
        return file(pathElements, content.getBytes(StandardCharsets.UTF_8))
    }

    FileWithState file(Iterable<String> pathElements, byte[] content) {
        def file = rootDirectory.toPath().resolve(pathElements.join(File.separator))
        def fileWithState = new FileWithState(file, content)
        fileWithState.create()
        return fileWithState
    }
}
