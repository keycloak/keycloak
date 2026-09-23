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

package org.keycloak.encoding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.keycloak.common.Version;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GzipResourceEncodingProviderTest {

    private static final String KC_TMPDIR = "kc.io.tmpdir";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String previousTmpDir;

    @Before
    public void before() throws IOException {
        previousTmpDir = System.getProperty(KC_TMPDIR);
        System.setProperty(KC_TMPDIR, temporaryFolder.newFolder("tmp").getAbsolutePath());
    }

    @After
    public void after() {
        if (previousTmpDir == null) {
            System.clearProperty(KC_TMPDIR);
        } else {
            System.setProperty(KC_TMPDIR, previousTmpDir);
        }
    }

    @Test
    public void changedResourceIsServedAfterRestart() throws IOException {
        GzipResourceEncodingProviderFactory factory = new GzipResourceEncodingProviderFactory();
        assertEquals("VERSION-ONE", encode(factory.create(null), "VERSION-ONE"));
        // the cached resource is served while the server is running, even if the theme resource changed
        assertEquals("VERSION-ONE", encode(factory.create(null), "VERSION-TWO"));

        // a new factory simulates a restart after the theme resource was changed
        assertEquals("VERSION-TWO", encode(newProvider(), "VERSION-TWO"));
    }

    @Test
    public void deletedResourceIsNotServedAfterRestart() throws IOException {
        assertEquals("VERSION-ONE", encode(newProvider(), "VERSION-ONE"));

        // a new factory simulates a restart after the theme resource was deleted
        assertNull(newProvider().getEncodedStream(() -> null, "login", "mytheme", "css", "test-cache.css"));
    }

    @Test
    public void staleResourceIsNotServedWhenCacheCannotBeCleared() throws IOException {
        assertEquals("VERSION-ONE", encode(newProvider(), "VERSION-ONE"));

        // make the cached resource undeletable, so clearing the cache on restart fails
        Path cssDir = Paths.get(System.getProperty(KC_TMPDIR), "kc-gzip-cache", Version.RESOURCES_VERSION, "login", "mytheme", "css");
        Assume.assumeTrue(Files.getFileStore(cssDir).supportsFileAttributeView(PosixFileAttributeView.class));
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(cssDir);
        Files.setPosixFilePermissions(cssDir, PosixFilePermissions.fromString("r-xr-xr-x"));
        try {
            // e.g. when running as root, the file can still be deleted
            Assume.assumeFalse(Files.isWritable(cssDir));

            // a new factory simulates a restart after the theme resource was changed
            assertEquals("VERSION-TWO", encode(newProvider(), "VERSION-TWO"));
        } finally {
            Files.setPosixFilePermissions(cssDir, permissions);
        }
    }

    private static ResourceEncodingProvider newProvider() {
        return new GzipResourceEncodingProviderFactory().create(null);
    }

    private static String encode(ResourceEncodingProvider provider, String content) throws IOException {
        InputStream encoded = provider.getEncodedStream(() -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "login", "mytheme", "css", "test-cache.css");
        try (InputStream is = new GZIPInputStream(encoded)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
