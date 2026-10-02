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
import java.util.zip.GZIPInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

// See https://github.com/keycloak/keycloak/issues/51066 and
// https://github.com/keycloak/keycloak/issues/52802
public class GzipResourceEncodingProviderFactoryTest {

    private static final String KC_TMPDIR = "kc.io.tmpdir";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String previousTmpDir;

    @Before
    public void before() throws IOException {
        previousTmpDir = System.getProperty(KC_TMPDIR);
        System.setProperty(KC_TMPDIR, temporaryFolder.newFolder().getAbsolutePath());
    }

    @After
    public void after() {
        if (previousTmpDir == null) {
            System.getProperties().remove(KC_TMPDIR);
        } else {
            System.setProperty(KC_TMPDIR, previousTmpDir);
        }
    }

    @Test
    public void clearCacheRemovesCachedEncodedFiles() throws IOException {
        GzipResourceEncodingProviderFactory factory = new GzipResourceEncodingProviderFactory();
        ResourceEncodingProvider provider = factory.create(null);

        assertEquals("VERSION-ONE", encode(provider, "VERSION-ONE"));

        // the encoded resource is cached, so a changed source is not picked up yet
        assertEquals("VERSION-ONE", encode(provider, "VERSION-TWO"));

        factory.clearCache();

        assertEquals("VERSION-TWO", encode(provider, "VERSION-TWO"));
    }

    private static String encode(ResourceEncodingProvider provider, String content) throws IOException {
        InputStream encoded = provider.getEncodedStream(() -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "login", "mytheme", "css", "test-cache.css");
        try (InputStream is = new GZIPInputStream(encoded)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
