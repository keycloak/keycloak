package org.keycloak.testframework.util;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;


public class JarUtil {

    /**
     * Builds a JAR from already-compiled classes and resources.
     *
     * @param jarName the JAR filename
     * @param classesPath path to compiled output directory ({@code target/classes})
     * @param targetPath path where to export the JAR
     */
    public static void buildModuleJar(String jarName, Path classesPath, Path targetPath) {
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
