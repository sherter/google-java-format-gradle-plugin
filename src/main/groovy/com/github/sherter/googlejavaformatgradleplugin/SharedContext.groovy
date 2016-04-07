package com.github.sherter.googlejavaformatgradleplugin

class SharedContext {

    final FileStateHandler fileStateHandler

    SharedContext(FileStateHandler fileStateHandler) {
        this.fileStateHandler = Objects.requireNonNull(fileStateHandler);
    }
}
