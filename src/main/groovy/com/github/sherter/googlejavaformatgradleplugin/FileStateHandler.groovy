package com.github.sherter.googlejavaformatgradleplugin

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import static groovy.transform.PackageScopeTarget.*

@CompileStatic
@PackageScope([CLASS, CONSTRUCTORS, FIELDS, METHODS])
class FileStateHandler {

    private static final Logger logger = Logging.getLogger('google-java-format')
    private static final String stateFileName = 'filestates.bin'

    private final File projectDir
    private final File buildCacheDir
    private final String currentGoogleJavaFormatVersion
    private final ConcurrentMap<String, FileInfo> fileStates = new ConcurrentHashMap<>()
    private final ConcurrentMap<String, Object> formattedFiles = new ConcurrentHashMap<>()

    FileStateHandler(File projectDir, File buildCacheDir, String currentGoogleJavaFormatVersion) {
        this.projectDir = projectDir
        this.buildCacheDir = buildCacheDir
        this.currentGoogleJavaFormatVersion = currentGoogleJavaFormatVersion
    }

    void load() {
        File statesFile = new File(buildCacheDir, stateFileName)
        if (!statesFile.exists()) {
            return
        }
        try {
            statesFile.withObjectInputStream(getClass().classLoader) { ObjectInputStream ois ->
                def deserializedObject = (Map<String, FileInfo>) ois.readObject()
                this.fileStates.putAll(deserializedObject)
            }
        } catch (IOException e) {
            logger.log(LogLevel.WARN, "Reading file states failed")
            statesFile.delete()
        }
    }

    void flush() {
        buildCacheDir.mkdirs()
        if (!buildCacheDir.isDirectory()) {
            logger.log(LogLevel.WARN, "Couldn't create directory ${buildCacheDir}.")
            return
        }

        File statesFile = new File(buildCacheDir, stateFileName)
        try {
            statesFile.withObjectOutputStream { oos ->
                oos.writeObject(fileStates)
            }
        } catch (IOException e) {
            logger.log(LogLevel.WARN, "Couldn't persist file state information.", e)
        }
    }

    boolean isUpToDate(File file) {
        String relativePath = projectRelativePath(file)
        FileInfo fileInfo = fileStates.get(relativePath)
        if (fileInfo == null || !fileInfo.googleJavaFormatVersion.equals(this.currentGoogleJavaFormatVersion)) {
            return false
        }
        Path path = Paths.get(file.absolutePath)
        long currentFileSize = Files.size(path)
        long currentLastModifiedTime = Files.getLastModifiedTime(path).toMillis()
        if (fileInfo.fileSize == currentFileSize
                && fileInfo.lastModified == currentLastModifiedTime) {
            return true
        }
        byte[] currentFileHash = calculateFileHash(path)
        if (Arrays.equals(fileInfo.fileHash, currentFileHash)) {
            fileStates.put(relativePath,
                    new FileInfo(
                            currentGoogleJavaFormatVersion,
                            currentFileSize,
                            currentLastModifiedTime,
                            currentFileHash))
            return true
        }
        return false
    }

    private String projectRelativePath(File file) {
        return projectDir.absoluteFile.toURI().relativize(file.absoluteFile.toURI()).toString()
    }

    private byte[] calculateFileHash(Path file) {
        return MessageDigest.getInstance('MD5').digest(Files.readAllBytes(file))
    }

    void formatIfNotUpToDate(File file, Closure formatFunction) {
        String projectRelativePath = projectRelativePath(file)
        Object putResult = formattedFiles.putIfAbsent(projectRelativePath, this)
        if (putResult == null) {
            formatFunction.call(file)
            Path path = Paths.get(file.absolutePath)
            fileStates.put(projectRelativePath, new FileInfo(
                    currentGoogleJavaFormatVersion,
                    Files.size(path),
                    Files.getLastModifiedTime(path).toMillis(),
                    calculateFileHash(path)))
        }
    }

    @PackageScope([CLASS, CONSTRUCTORS])
    static class FileInfo implements Serializable {
        private final String googleJavaFormatVersion
        private final long fileSize
        private final long lastModified
        private final byte[] fileHash

        FileInfo(String googleJavaFormatVersion, long fileSize, long lastModified, byte[] fileHash) {
            this.googleJavaFormatVersion = googleJavaFormatVersion
            this.fileSize = fileSize
            this.lastModified = lastModified
            this.fileHash = fileHash
        }
    }
}
