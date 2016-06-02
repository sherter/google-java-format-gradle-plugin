package com.github.sherter.googlejavaformatgradleplugin.test

import java.nio.charset.StandardCharsets
import java.nio.file.Path

class Project {

    final Path rootDirectory

    Project(File projectDir) {
        this.rootDirectory = projectDir.toPath()
    }

    /**
     * Creates and returns a {@link FileWithState} from the given path elements
     * relative to the project directory. If a parent directory does not exist,
     * it will be created automatically.
     */
    FileWithState createFile(Iterable<String> pathElements) {
        def file = rootDirectory.resolve(pathElements.join(File.separator))
        return FileWithState.create(file)
    }

    /**
     * This method is for convenience only. First creates a {@link FileWithState} and
     * then writes some initial content.
     */
    FileWithState createFile(Iterable<String> pathElements, byte[] content) {
        def fileWithState = createFile(pathElements)
        fileWithState.write(content)
        return fileWithState
    }

    /**
     * This method is for convinience only. First creates a {@link FileWithState} and
     * then writes the content encoded as UTF8.
     */
    FileWithState createFile(Iterable<String> pathElements, String content) {
        return createFile(pathElements, content.getBytes(StandardCharsets.UTF_8))
    }
}
