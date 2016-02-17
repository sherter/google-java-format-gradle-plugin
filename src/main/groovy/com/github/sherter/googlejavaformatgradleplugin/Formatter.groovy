package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic

@CompileStatic
interface Formatter {

    String format(String source) throws FormatterException

}
