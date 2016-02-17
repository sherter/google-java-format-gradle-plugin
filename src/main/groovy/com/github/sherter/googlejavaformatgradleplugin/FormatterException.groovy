package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
class FormatterException extends Exception {

    final Collection<ErrorInfo> errors;

    FormatterException(Collection<ErrorInfo> errors) {
        this.errors = Collections.unmodifiableCollection(errors.collect())
    }

    @Immutable
    static class ErrorInfo {
        int line
        int column
        String message

        String toString() {
            return "$line:$column: $message"
        }
    }
}
