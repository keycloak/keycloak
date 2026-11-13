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
package org.keycloak.saml.processing.core.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.mustcall.qual.Owning;

final class SchemaInputStreams implements Closeable {

    @SuppressWarnings({"type.argument", "type.arguments.not.inferred"}) // ResourceLeakChecker cannot model resource collections (manual section 8.10).
    private final List<InputStream> streams = new ArrayList<>();

    @SuppressWarnings("required.method.not.called") // Ownership transfers to the collection; close() closes every element.
    void add(@Owning InputStream stream) {
        streams.add(stream);
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (int i = streams.size() - 1; i >= 0; i--) {
            try {
                streams.get(i).close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else if (failure != e) {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
