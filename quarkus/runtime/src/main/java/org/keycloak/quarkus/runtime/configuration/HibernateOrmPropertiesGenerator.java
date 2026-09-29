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
package org.keycloak.quarkus.runtime.configuration;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TreeMap;

import org.keycloak.quarkus.runtime.configuration.HibernateOrmProperties.HibernateOrmProperty;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClass;

/**
 * Generates {@value HibernateOrmProperties#RESOURCE} when this module is built, see the {@code exec-maven-plugin} execution in
 * the pom. Like Quarkus, it finds the config roots of the Hibernate ORM extension in {@value #CONFIG_ROOTS}, reads their
 * {@link ConfigRoot} and {@link ConfigMapping} annotations, and lets SmallRye Config list the property names. The build time
 * root is in the deployment module of the extension, a {@code provided} dependency of this module without transitive
 * dependencies: it is neither packaged nor inherited by the server. Map keys such as
 * {@code quarkus.hibernate-orm.unsupported-properties."..."} and deprecated properties are excluded.
 */
public final class HibernateOrmPropertiesGenerator {

    /**
     * The file in which a Quarkus extension lists its config roots, see {@code io.quarkus.deployment.configuration.BuildTimeConfigurationReader}.
     */
    static final String CONFIG_ROOTS = "META-INF/quarkus-config-roots.list";

    private HibernateOrmPropertiesGenerator() {
    }

    /**
     * @param args the output directory
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the output directory as the only argument");
        }
        Map<String, HibernateOrmProperty> properties = discover(HibernateOrmPropertiesGenerator.class.getClassLoader());
        if (properties.values().stream().noneMatch(HibernateOrmProperty::buildTime)
                || properties.values().stream().allMatch(HibernateOrmProperty::buildTime)) {
            throw new IllegalStateException("Failed to collect the build time and the run time properties of the Quarkus Hibernate ORM extension: " + properties.keySet());
        }

        byte[] serialized = serialize(properties.values());
        if (!properties.equals(HibernateOrmProperties.parse(new ByteArrayInputStream(serialized)))) {
            throw new IllegalStateException("The serialized Hibernate ORM properties do not parse back to the collected ones");
        }

        Path output = Paths.get(args[0]).resolve(HibernateOrmProperties.RESOURCE);
        Files.createDirectories(output.getParent());
        Files.write(output, serialized);
        System.out.printf("Generated %s with %d Quarkus Hibernate ORM properties%n", output, properties.size());
    }

    /**
     * Collects the properties of the Hibernate ORM config roots loadable from the class loader.
     */
    public static Map<String, HibernateOrmProperty> discover(ClassLoader classLoader) {
        List<Class<?>> configRoots = new ArrayList<>();
        try {
            Enumeration<URL> configRootLists = classLoader.getResources(CONFIG_ROOTS);
            while (configRootLists.hasMoreElements()) {
                for (String className : readLines(configRootLists.nextElement())) {
                    Class<?> configRoot;
                    try {
                        configRoot = Class.forName(className, false, classLoader);
                    } catch (ClassNotFoundException | LinkageError e) {
                        continue; // a root of an extension whose dependencies are absent
                    }
                    ConfigMapping mapping = configRoot.getAnnotation(ConfigMapping.class);
                    if (mapping != null && HibernateOrmProperties.PREFIX.equals(mapping.prefix())) {
                        configRoots.add(configRoot);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + CONFIG_ROOTS, e);
        }
        return collect(configRoots);
    }

    /**
     * The properties of the given config roots. A property defined by both a build time and a run time root is a run time
     * property.
     */
    static Map<String, HibernateOrmProperty> collect(Collection<Class<?>> configRoots) {
        Map<String, HibernateOrmProperty> result = new TreeMap<>();
        for (Class<?> configRoot : configRoots) {
            ConfigMapping mapping = Objects.requireNonNull(configRoot.getAnnotation(ConfigMapping.class), "Not a config mapping: " + configRoot);
            // like Quarkus, see BuildTimeConfigurationReader, a config root without a phase is a build time config root
            ConfigRoot root = configRoot.getAnnotation(ConfigRoot.class);
            boolean buildTime = root == null || root.phase() != ConfigPhase.RUN_TIME;

            Map<String, Property> mappingProperties = ConfigMappings.getProperties(ConfigClass.configClass(configRoot, mapping.prefix()));
            for (Map.Entry<String, Property> entry : mappingProperties.entrySet()) {
                String name = entry.getKey();
                Property property = entry.getValue();
                if (!name.startsWith(HibernateOrmProperties.PREFIX + ".") || name.contains("*") || property.isGroup()
                        || property.getMethod().isAnnotationPresent(Deprecated.class)) {
                    // map keys (quarkus.hibernate-orm.*.log.sql, unsupported-properties.*) and collection elements
                    // (packages[*]) have no fixed name
                    continue;
                }
                String suffix = name.substring(HibernateOrmProperties.PREFIX.length() + 1);
                // a persistence unit property exists for the default unit and for any named unit
                boolean perUnit = mappingProperties.containsKey(HibernateOrmProperties.PREFIX + ".*." + suffix);
                result.merge(name, new HibernateOrmProperty(name, buildTime, perUnit, typeOf(property)),
                        (existing, added) -> existing.buildTime() ? added : existing);
            }
        }
        return result;
    }

    private static Class<?> typeOf(Property property) {
        Class<?> type = null;
        if (property.isPrimitive()) {
            Class<?> primitive = property.asPrimitive().getPrimitiveType();
            if (primitive == boolean.class) {
                type = Boolean.class;
            } else if (primitive == int.class) {
                type = Integer.class;
            } else if (primitive == long.class) {
                type = Long.class;
            }
        } else if (property.isLeaf()) { // including an optional leaf
            type = property.asLeaf().getValueRawType();
            if (type == OptionalInt.class) {
                type = Integer.class;
            } else if (type == OptionalLong.class) {
                type = Long.class;
            }
        }
        if (type == Boolean.class || type == Integer.class || type == Long.class) {
            return type;
        }
        return String.class;
    }

    /**
     * Serializes the properties to the format of {@link HibernateOrmProperties#RESOURCE}, sorted by name.
     */
    public static byte[] serialize(Collection<HibernateOrmProperty> properties) {
        Map<String, HibernateOrmProperty> sorted = new TreeMap<>();
        properties.forEach(property -> sorted.put(property.name(), property));

        StringBuilder result = new StringBuilder("# Auto-generated, DO NOT change this file: the Quarkus Hibernate ORM properties collected during the build\n");
        sorted.forEach((name, property) -> result.append(name).append('=')
                .append(property.buildTime() ? HibernateOrmProperties.BUILD_TIME : HibernateOrmProperties.RUN_TIME).append(',')
                .append(property.perUnit() ? HibernateOrmProperties.PERSISTENCE_UNIT : HibernateOrmProperties.GLOBAL).append(',')
                .append(property.type().getSimpleName().toLowerCase())
                .append('\n'));
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static Iterable<String> readLines(URL url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            return reader.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
        }
    }
}
