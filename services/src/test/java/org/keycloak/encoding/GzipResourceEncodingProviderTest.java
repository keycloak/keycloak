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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GzipResourceEncodingProviderTest {

    private static final int THREADS = 16;

    @Rule
    public TemporaryFolder cacheDir = new TemporaryFolder();

    @Test
    public void failedAtomicMoveReturnsEncodedStreamAndCleansTemporaryFile() throws Exception {
        GzipResourceEncodingProvider provider = new GzipResourceEncodingProvider(cacheDir.getRoot());
        byte[] content = "body { color: red; }".getBytes(StandardCharsets.UTF_8);
        Path target = cacheDir.getRoot().toPath().resolve("style.css.gz");
        try (InputStream encoded = provider.getEncodedStream(() -> {
            // A directory at the destination prevents publishing the completed file
            Files.createDirectory(target);
            return new ByteArrayInputStream(content);
        }, "style.css")) {
            assertNotNull("encoded stream missing after failed move", encoded);
            try (GZIPInputStream gzip = new GZIPInputStream(encoded)) {
                assertArrayEquals(content, gzip.readAllBytes());
            }
        }
        assertArrayEquals(new String[]{"style.css.gz"}, cacheDir.getRoot().list());
    }

    @Test
    public void concurrentRequestsForUncachedResourceReturnValidEncodedStreams() throws Exception {
        GzipResourceEncodingProvider provider = new GzipResourceEncodingProvider(cacheDir.getRoot());
        byte[] content = "body { color: red; }\n".repeat(4096).getBytes(StandardCharsets.UTF_8);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            int encodedResponses = 0;
            // Each round uses a new resource name, so all threads race on a cold cache
            for (int round = 0; round < 100; round++) {
                String resource = "style-" + round + ".css";
                CyclicBarrier barrier = new CyclicBarrier(THREADS);
                List<Future<InputStream>> results = new ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    results.add(executor.submit(() -> {
                        barrier.await();
                        return provider.getEncodedStream(() -> new ByteArrayInputStream(content), "login", "test", resource);
                    }));
                }
                for (Future<InputStream> result : results) {
                    try (InputStream encoded = result.get(30, TimeUnit.SECONDS)) {
                        // ThemeResource serves the original resource when encoding fails
                        if (encoded != null) {
                            encodedResponses++;
                            assertArrayEquals(content, new GZIPInputStream(encoded).readAllBytes());
                        }
                    }
                }
                try (InputStream cached = provider.getEncodedStream(() -> {
                    throw new AssertionError("Cached resource should not be encoded again");
                }, "login", "test", resource)) {
                    assertNotNull("cached stream missing for " + resource, cached);
                    try (GZIPInputStream gzip = new GZIPInputStream(cached)) {
                        assertArrayEquals(content, gzip.readAllBytes());
                    }
                }
            }
            assertTrue("No encoded streams returned", encodedResponses > 0);
        } finally {
            executor.shutdownNow();
        }
    }
}
