/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.jdbc;

import java.io.IOException;
import java.io.InputStream;

import org.keycloak.representations.migration.MigrationManifest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads the {@link MigrationManifest} from the classpath resource
 * {@value #MANIFEST_RESOURCE}.
 */
public class MigrationManifestLoader {

    static final String MANIFEST_RESOURCE = "META-INF/schema/migration-manifest.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static volatile MigrationManifest cached;

    /**
     * Returns the manifest, loading it from the classpath on the first call and caching it thereafter.
     *
     * @throws IllegalStateException when the manifest resource is not found or cannot be parsed
     */
    public static MigrationManifest load() {
        if (cached == null) {
            synchronized (MigrationManifestLoader.class) {
                if (cached == null) {
                    cached = loadFromClasspath();
                }
            }
        }
        return cached;
    }

    /** Loads a fresh copy (used in tests to avoid cross-test caching). */
    static MigrationManifest loadFresh() {
        return loadFromClasspath();
    }

    private static MigrationManifest loadFromClasspath() {
        InputStream is = MigrationManifestLoader.class.getClassLoader().getResourceAsStream(MANIFEST_RESOURCE);
        if (is == null) {
            throw new IllegalStateException("Migration manifest not found on classpath: " + MANIFEST_RESOURCE);
        }
        try (is) {
            return MAPPER.readValue(is, MigrationManifest.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load migration manifest: " + MANIFEST_RESOURCE, e);
        }
    }

    private MigrationManifestLoader() {
    }
}
