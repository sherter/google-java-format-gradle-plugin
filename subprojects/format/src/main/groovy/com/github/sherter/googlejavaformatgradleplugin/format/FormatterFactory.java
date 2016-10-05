package com.github.sherter.googlejavaformatgradleplugin.format;

interface FormatterFactory {

  Formatter create(FormatterOption[] options);

  Formatter create() throws ReflectiveOperationException;
}
