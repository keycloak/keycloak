package org.keycloak.testframework.util;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.keycloak.it.utils.Maven;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
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
            throw new RuntimeException(e);
        }

        providerJar.as(ZipExporter.class).exportTo(targetPath.toFile(), true);
    }
}
