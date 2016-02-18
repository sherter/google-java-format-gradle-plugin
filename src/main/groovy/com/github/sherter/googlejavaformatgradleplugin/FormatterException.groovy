package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic

@CompileStatic
class FormatterException extends Exception {

    final Collection<ErrorInfo> errors;

    FormatterException(Collection<ErrorInfo> errors) {
        this.errors = Collections.unmodifiableCollection(errors.collect())
    }

    static class ErrorInfo {
        final int line
        final int column
        final String message

        ErrorInfo(int line, int column, String message) {
            this.line = line
            this.column = column
            this.message = message
        }

        String toString() {
            return "$line:$column: $message"
        }
    }
}
