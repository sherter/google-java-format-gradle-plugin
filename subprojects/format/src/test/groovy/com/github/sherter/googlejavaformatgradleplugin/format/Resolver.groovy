package com.github.sherter.googlejavaformatgradleplugin.format

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes

class Resolver {

    private static final RemoteRepository MAVEN_CENTRAL_REPOSITORY = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build()
    private static final LocalRepository LOCAL_REPOSITORY = new LocalRepository(System.getProperty("maven.repo.local"))

    private static final RepositorySystem system = createRepositorySystem()
    private static final RepositorySystemSession session = createRepositorySystemSession(system)

    static ClassLoader resolve(String version) {
        DependencyRequest request = createGoogleJavaFormatRequest(version)
        List<ArtifactResult> artifactResults = system.resolveDependencies(session, request).getArtifactResults()

        URL[] artifactLocations = artifactResults.collect {
            result -> result.getArtifact().getFile().toURI().toURL()
        }
        URLClassLoader classLoader = new URLClassLoader(artifactLocations, (ClassLoader) null)
        return classLoader
    }


    private static DependencyRequest createGoogleJavaFormatRequest(String version) {
        def coordinates = new DefaultArtifact("${Gjf.GROUP_ID}:${Gjf.ARTIFACT_ID}:${version}")
        def collectRequest = new CollectRequest()
                .setRoot(new Dependency(coordinates, JavaScopes.RUNTIME))
                .setRepositories([MAVEN_CENTRAL_REPOSITORY])
        return new DependencyRequest().setCollectRequest(collectRequest)
    }

    private static RepositorySystem createRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
        locator.addService(TransporterFactory.class, FileTransporterFactory.class)
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class)

        return locator.getService(RepositorySystem.class)
    }

    private static RepositorySystemSession createRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession()
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER)
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, LOCAL_REPOSITORY))
        return session
    }
}
