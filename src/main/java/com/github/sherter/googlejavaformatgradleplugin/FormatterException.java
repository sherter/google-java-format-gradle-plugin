package com.github.sherter.googlejavaformatgradleplugin;

import com.google.common.collect.ImmutableList;

import java.util.Collection;

class FormatterException extends Exception {

  private final ImmutableList<FormatterDiagnostic> diagnostics;

  static FormatterException create(Collection<FormatterDiagnostic> diagnostics) {
    String numProblem;
    if (diagnostics.size() == 1) {
      numProblem = "a problem";
    } else {
      numProblem = diagnostics.size() + " problems";
    }
    return new FormatterException(
        diagnostics, numProblem + " prevented formatting from succeeding");
  }

  private FormatterException(Collection<FormatterDiagnostic> diagnostics, String message) {
    super(message);
    this.diagnostics = ImmutableList.copyOf(diagnostics);
  }

  ImmutableList<FormatterDiagnostic> diagnostics() {
    return diagnostics;
  }
}
