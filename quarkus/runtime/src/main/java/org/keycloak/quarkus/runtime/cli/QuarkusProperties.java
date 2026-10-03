package org.keycloak.quarkus.runtime.cli;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.util.ClassPathUtils;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappings;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.PropertyNamesMatcher;

import static io.smallrye.config.ConfigMappings.ConfigClass.configClass;

public class QuarkusProperties {

    private static final String CONFIG_ROOTS_LIST = "META-INF/quarkus-config-roots.list";

    private static Iterable<Class<?>> classesNamedIn(ClassLoader classLoader, String fileName)
            throws IOException, ClassNotFoundException {
        final ArrayList<Class<?>> list = new ArrayList<>();
        for (String className : classNamesNamedIn(classLoader, fileName)) {
            list.add(Class.forName(className, true, classLoader));
        }
        return Collections.unmodifiableList(list);
    }

    private static Set<String> classNamesNamedIn(ClassLoader classLoader, String fileName) {
        final Set<String> classNames = new LinkedHashSet<>();
        try {
            ClassPathUtils.consumeAsStreams(classLoader, fileName, classFile -> {
                try (InputStreamReader reader = new InputStreamReader(classFile, StandardCharsets.UTF_8)) {
                    try (BufferedReader br = new BufferedReader(reader)) {
                        readStream(classNames, br);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            // TODO: could confirm this is indicative of a provider jar going missing
        }
        return Collections.unmodifiableSet(classNames);
    }

    /**
     * - Lines starting by a # (or white spaces and a #) are ignored. - For
     * lines containing data before a comment (#) are parsed and only the value
     * before the comment is used.
     */
    private static void readStream(final Set<String> classNames, final BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            int commentMarkerIndex = line.indexOf('#');
            if (commentMarkerIndex >= 0) {
                line = line.substring(0, commentMarkerIndex);
            }
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            classNames.add(line);
        }
    }

    private static List<Class<?>> collectConfigRoots(ClassLoader classLoader) {
        // populate with all known types
        List<Class<?>> roots = new ArrayList<>();
        try {
            for (Class<?> clazz : classesNamedIn(classLoader, CONFIG_ROOTS_LIST)) {
                if (!clazz.isInterface()) {
                    throw new IllegalArgumentException(
                            "The configuration " + clazz + " must be an interface annotated with @ConfigRoot and @ConfigMapping");
                }

                ConfigRoot configRoot = clazz.getAnnotation(ConfigRoot.class);
                if (configRoot == null) {
                    throw new IllegalArgumentException("The configuration " + clazz + " is missing the @ConfigRoot annotation");
                }

                ConfigMapping configMapping = clazz.getAnnotation(ConfigMapping.class);
                if (configMapping == null) {
                    throw new IllegalArgumentException("The configuration " + clazz + " is missing the @ConfigMapping annotation");
                }

                roots.add(clazz);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return roots;
    }

    public static PropertyNamesMatcher readBuildTimeConfiguration() {
        var configRoots = collectConfigRoots(QuarkusProperties.class.getClassLoader());

        List<ConfigClass> buildTimeMappings = new ArrayList<>();

        for (Class<?> configRoot : configRoots) {
            boolean isMapping = configRoot.isAnnotationPresent(ConfigMapping.class);
            if (isMapping) {
                ConfigPhase phase = ConfigPhase.BUILD_TIME;
                // To retrieve config phase
                ConfigRoot annotation = configRoot.getAnnotation(ConfigRoot.class);
                if (annotation != null) {
                    phase = annotation.phase();
                }

                ConfigClass mapping = configClass(configRoot);
                if (phase.equals(ConfigPhase.BUILD_TIME) || phase.equals(ConfigPhase.BUILD_AND_RUN_TIME_FIXED)) {
                    buildTimeMappings.add(mapping);
                }
            }
        }

        return ConfigMappings.propertyNamesMatcher(buildTimeMappings);
    }

}
