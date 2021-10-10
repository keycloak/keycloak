/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.configuration;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.smallrye.config.PropertiesConfigSource;

import static org.keycloak.common.util.StringPropertyReplacer.replaceProperties;
import static org.keycloak.configuration.MicroProfileConfigProvider.NS_KEYCLOAK;
import static org.keycloak.configuration.MicroProfileConfigProvider.NS_QUARKUS;

/**
 * A configuration source for {@code keycloak.properties}.
 */
public abstract class KeycloakPropertiesConfigSource extends PropertiesConfigSource {

    private static final Logger log = Logger.getLogger(KeycloakPropertiesConfigSource.class);

    private static final Pattern DOT_SPLIT = Pattern.compile("\\.");
    static final String KEYCLOAK_PROPERTIES = "keycloak.properties";

    KeycloakPropertiesConfigSource(InputStream is, int ordinal) {
        super(readProperties(is), KEYCLOAK_PROPERTIES, ordinal);
    }

    private static Map<String, String> readProperties(final InputStream is) {
        if (is == null) {
            return Collections.emptyMap();
        }
        try (Closeable ignored = is) {
            Properties properties = new Properties();
            properties.load(is);
            return transform((Map<String, String>) (Map) properties);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static final class InJar extends KeycloakPropertiesConfigSource {
        public InJar() {
            super(openStream(), 245);
        }

        private static InputStream openStream() {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = KeycloakPropertiesConfigSource.class.getClassLoader();
            }
            InputStream is;
            String fileName = "META-INF/" + KEYCLOAK_PROPERTIES;
            if (cl == null) {
                is = ClassLoader.getSystemResourceAsStream(fileName);
            } else {
                is = cl.getResourceAsStream(fileName);
            }
            if (is != null) {
                log.debug("Loading the server configuration from the classpath");
            }
            return is;
        }
    }

    public static final class InFileSystem extends KeycloakPropertiesConfigSource {

        public InFileSystem(Path path) {
            super(openStream(path), 255);
        }

        private static InputStream openStream(Path path) {
            if (path == null) {
                throw new IllegalArgumentException("Configuration file path can not be null");
            }
            try {
                log.debugf("Loading the server configuration from %s", path);
                return Files.newInputStream(path);
            } catch (NoSuchFileException | FileNotFoundException e) {
                throw new IllegalArgumentException("Configuration file not found at [" + path + "]");
            } catch (IOException e) {
                throw new RuntimeException("Unexpected error reading configuration file at [" + path + "]", e);
            }
        }
    }

    private static Map<String, String> transform(Map<String, String> properties) {
        Map<String, String> result = new HashMap<>(properties.size());
        properties.keySet().forEach(k -> result.put(transformKey(k), replaceProperties(properties.get(k))));
        return result;
    }

    /**
     * We need a better namespace resolution so that we don't need to add Quarkus extensions manually. Maybe the easiest 
     * path is to just have the "kc" namespace for Keycloak-specific properties.
     * 
     * @param key the key to transform
     * @return the same key but prefixed with the namespace
     */
    private static String transformKey(String key) {
        String namespace;
        String[] keyParts = DOT_SPLIT.split(key);
        String extension = keyParts[0];
        String profile = "";
        String transformed = key;

        if (extension.startsWith("%")) {
            profile = String.format("%s.", keyParts[0]);
            extension = keyParts[1];
            transformed = key.substring(key.indexOf('.') + 1);
        }

        if (extension.equalsIgnoreCase(NS_QUARKUS)) {
            return key;
        } else {
            namespace = NS_KEYCLOAK;
        }

        return profile + namespace + "." + transformed;

    }
}
