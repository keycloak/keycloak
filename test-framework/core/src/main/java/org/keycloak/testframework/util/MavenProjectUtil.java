package org.keycloak.testframework.util;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.it.utils.Maven;
import org.keycloak.testframework.FatalTestClassException;
import org.keycloak.testframework.server.KeycloakDependency;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import org.eclipse.aether.artifact.Artifact;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public final class MavenProjectUtil {

    private static LocalProject rootModuleProject;

    private static LocalProject getRootModule() {
        if (rootModuleProject != null) {
            return rootModuleProject;
        }

        BootstrapMavenContext ctx = Maven.bootstrapCurrentMavenContext();
        LocalProject m = ctx.getCurrentProject();
        while (m.getLocalParent() != null) {
            m = m.getLocalParent();
        }
        rootModuleProject = m;
        return rootModuleProject;
    }

    public static LocalProject findLocalModule(String groupId, String artifactId) {
        LocalProject rootModule = getRootModule();
        LocalProject dependencyModule = rootModule.getWorkspace().getProject(groupId, artifactId);
        if (dependencyModule == null) {
            throw new RuntimeException("Failed to resolve artifact in this project: [" + groupId + ":" + artifactId + "]");
        }
        return dependencyModule;
    }

    public static LocalProject getCurrentModule() {
        BootstrapMavenContext ctx = Maven.bootstrapCurrentMavenContext();
        return ctx.getCurrentProject();
    }

    public static KeycloakDependency updateDependencyDetails(KeycloakDependency dependency) {
        KeycloakDependency.Builder updatedDependency = new KeycloakDependency.Builder()
                .hotDeployable(dependency.isHotDeployable())
                .dependencyCurrentProject(dependency.dependencyCurrentProject());

        if (dependency.dependencyCurrentProject()) {
            LocalProject localProject = getCurrentModule();

            updatedDependency
                    .setGroupId(localProject.getGroupId())
                    .setArtifactId(localProject.getArtifactId())
                    .setVersion(localProject.getVersion());
        } else {
            String version = Optional.ofNullable(Maven.getArtifact(dependency.getGroupId(), dependency.getArtifactId()))
                    .map(Artifact::getVersion)
                    .orElse("");

            updatedDependency
                    .setGroupId(dependency.getGroupId())
                    .setArtifactId(dependency.getArtifactId())
                    .setVersion(version);
        }

        return updatedDependency.build();
    }

    /**
     * Builds and exports a JAR from compiled classes and resources.
     *
     * @param jarName the JAR filename
     * @param classesPath path to compiled output directory ({@code target/classes})
     * @param targetPath path where to export the JAR
     */
    public static void buildJar(String jarName, Path classesPath, Path targetPath) {
        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class, jarName);

        try (Stream<Path> sourcePathStream = Files.walk(classesPath)) {
            sourcePathStream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        String relativeFilePath = classesPath.relativize(p).toString();

                        if (relativeFilePath.endsWith(".class")) {
                            String fullyQualifiedClassName = relativeFilePath.replace(File.separatorChar, '.').substring(0, relativeFilePath.lastIndexOf('.'));
                            providerJar.addClass(fullyQualifiedClassName);
                        } else {
                            File resourceFile = p.toFile();
                            providerJar.addAsResource(resourceFile, relativeFilePath);
                        }
                    });
        } catch (IOException e) {
            throw new FatalTestClassException("Could not build a JAR ", e);
        }

        providerJar.as(ZipExporter.class).exportTo(targetPath.toFile(), true);
    }

    /**
     * TODO add JavaDoc
     */
    public static void buildJar(String jarName, Path classesPath, Set<Class<?>> classesToDeploy, Set<String> servicesFiles, List<String> resourceFiles, Path targetPath) {
        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class, jarName);

        try (Stream<Path> sourcePathStream = Files.walk(classesPath)) {
            List<String> sources = sourcePathStream .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(f -> f.endsWith(".class"))
                    .map(s -> s.substring(0, s.lastIndexOf('.'))).toList();

            for (Class<?> clazz : classesToDeploy) {
                if (sources.contains(clazz.getSimpleName())) {
                    providerJar.addClass(clazz);
                }
            }
        } catch (IOException e) {
            throw new FatalTestClassException("Could not build a JAR ", e);
        }

        // TODO when service file has multiple providers but not all are deployed Keycloak fails to start
        for (String r : servicesFiles) {
            Path serviceFilePath = classesPath.resolve(r);
            File serviceFile = serviceFilePath.toFile();
            if (serviceFile.exists() && serviceFile.isFile()) {
                providerJar.addAsResource(serviceFile, r);
            }
        }

        for (String resourceFile : resourceFiles) {
            Path resourceFilePath = classesPath.resolve(resourceFile);
            File serviceFile = resourceFilePath.toFile();
            if (!serviceFile.exists() || !serviceFile.isFile()) {
                throw new FatalTestClassException("Could not find \"" + resourceFile + "\" in: " + resourceFilePath);
            }
            providerJar.addAsResource(serviceFile, resourceFile);
        }

        providerJar.as(ZipExporter.class).exportTo(targetPath.toFile(), true);
    }
}
