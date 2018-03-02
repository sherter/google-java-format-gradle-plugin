package com.github.sherter.googlejavaformatgradleplugin.format

import groovy.grape.Grape

class Resolver {

    static ClassLoader resolve(String version) {
        ClassLoader loader = new GroovyClassLoader(ClassLoader.getSystemClassLoader())
        Grape.grab([classLoader: loader], [group: Gjf.GROUP_ID,
                                           module: Gjf.ARTIFACT_ID,
                                           version: version])
        return loader
    }
}
