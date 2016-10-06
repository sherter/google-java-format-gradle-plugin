package com.github.sherter.googlejavaformatgradleplugin.format;

interface FormatterFactory {

  Formatter create() throws ReflectiveOperationException;
}
