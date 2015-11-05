# google-java-format-gradle-plugin

This [gradle](https://github.com/gradle/gradle) plugin uses [google-java-format](https://github.com/google/google-java-format) to reformat the Java source files in your gradle project.

## Usage
[google-java-format](https://github.com/google/google-java-format) is not officially released yet. However, snapshot versions are available on sonatype's snapshot repository.

```groovy
plugins {
  id 'java'
  id 'com.github.sherter.google-java-format' version '0.1'
}

// The plugin does not add any repositories by itself. You can choose where google-java-format
// and its transitive dependencies should come from. The following is only an example. You may
// want to use mavenLocal(), if you compiled google-java-format by yourself.
repositories {
  maven {
    url 'https://oss.sonatype.org/content/repositories/snapshots/'
  }
  jcenter()	
}

// the following block is optional; toolVersion defaults to 0.1-SNAPSHOT
googleJavaFormat {
  toolVersion = '0.1-SNAPSHOT'
}
```

The task `googleJavaFormat` will format all Java sources in your project.
