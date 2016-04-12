package com.github.sherter.googlejavaformatgradleplugin;

import dagger.Component;

@Component(modules = PersistenceModule.class)
interface PersistenceComponent {
  FileInfoStore store();
}
