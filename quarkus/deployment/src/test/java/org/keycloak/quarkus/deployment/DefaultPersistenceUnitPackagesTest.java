/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.quarkus.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The default persistence unit declares its packages ({@code quarkus.hibernate-orm.packages} in the application.properties
 * of the server module), so that the persistence units of extensions can be defined through configuration. An entity of
 * Keycloak outside of these packages would only reach the default persistence unit through
 * {@link KeycloakProcessor#assignUnclaimedModelClasses}: this test keeps the list in sync with the entities.
 */
public class DefaultPersistenceUnitPackagesTest {

    private static final String PACKAGES_PROPERTY = "quarkus.hibernate-orm.packages";
    private static final DotName ENTITY = DotName.createSimple("jakarta.persistence.Entity");

    @Test
    public void defaultPersistenceUnitPackagesAreThePackagesOfTheEntities() throws IOException {
        Set<String> configured = getConfiguredPackages();
        Set<String> entityPackages = getEntityPackagesOfKeycloak();

        assertEquals(entityPackages, configured, "Set " + PACKAGES_PROPERTY + " in the application.properties of the server module to the packages of the entities");
    }

    private static Set<String> getConfiguredPackages() throws IOException {
        Set<String> packages = null;
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("application.properties");
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            Properties properties = new Properties();
            try (InputStream in = resource.openStream()) {
                properties.load(in);
            }
            String value = properties.getProperty(PACKAGES_PROPERTY);
            if (value != null) {
                assertNull(packages, PACKAGES_PROPERTY + " is set in several application.properties, the second one is " + resource);
                packages = new TreeSet<>();
                for (String pkg : value.split(",")) {
                    packages.add(pkg.trim());
                }
            }
        }
        assertNotNull(packages, PACKAGES_PROPERTY + " is not set in the application.properties of the server module");
        return packages;
    }

    /**
     * The packages of the classes annotated with {@code @Entity} in the Keycloak jars of the test class path.
     */
    private static Set<String> getEntityPackagesOfKeycloak() throws IOException {
        Indexer indexer = new Indexer();
        int jars = 0;
        // surefire runs the tests with a manifest-only jar, and lists the real class path in a property of its own
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        for (String entry : classPath.split(File.pathSeparator)) {
            Path path = Paths.get(entry);
            String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
            if (!fileName.startsWith("keycloak-") || !fileName.endsWith(".jar")) {
                continue;
            }
            jars++;
            try (JarFile jar = new JarFile(path.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    if (jarEntry.getName().endsWith(".class") && !jarEntry.getName().startsWith("META-INF/")) {
                        try (InputStream in = jar.getInputStream(jarEntry)) {
                            indexer.index(in);
                        }
                    }
                }
            }
        }
        assertFalse(jars == 0, "No Keycloak jar found in the class path " + classPath);

        Index index = indexer.complete();
        Set<String> packages = new TreeSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(ENTITY)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                packages.add(annotation.target().asClass().name().packagePrefix());
            }
        }
        assertFalse(packages.isEmpty(), "No entity found in the Keycloak jars of the class path");
        return packages;
    }
}
