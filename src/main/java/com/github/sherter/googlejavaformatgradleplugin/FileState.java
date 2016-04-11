package com.github.sherter.googlejavaformatgradleplugin;

enum FileState {
  FORMATTED,
  UNFORMATTED,
  INVALID,
  UNKNOWN;

  static final FileState values[] = values();
}
