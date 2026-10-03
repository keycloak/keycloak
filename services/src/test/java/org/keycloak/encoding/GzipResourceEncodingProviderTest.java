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

public class GzipResourceEncodingProviderTest {

    private static final int THREADS = 16;

    @Rule
    public TemporaryFolder cacheDir = new TemporaryFolder();

    @Test
    public void concurrentRequestsForUncachedResourceAllGetEncodedStream() throws Exception {
        GzipResourceEncodingProvider provider = new GzipResourceEncodingProvider(cacheDir.getRoot());
        byte[] content = "body { color: red; }\n".repeat(4096).getBytes(StandardCharsets.UTF_8);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
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
                        assertNotNull("encoded stream missing for " + resource, encoded);
                        assertArrayEquals(content, new GZIPInputStream(encoded).readAllBytes());
                    }
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
