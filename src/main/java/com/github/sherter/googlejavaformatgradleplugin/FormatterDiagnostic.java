package com.github.sherter.googlejavaformatgradleplugin;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class FormatterDiagnostic {

  static FormatterDiagnostic create(int line, int column, String message) {
    return new AutoValue_FormatterDiagnostic(line, column, message);
  }

  abstract int line();

  abstract int column();

  abstract String message();
}
