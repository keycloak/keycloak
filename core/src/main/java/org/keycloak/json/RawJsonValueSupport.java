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

package org.keycloak.json;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * SPI for {@link RawJsonValue} navigation behavior, allowing each Jackson version to provide
 * its own implementation backed by the real {@code JsonNode}.
 * <p>
 * Discovered via {@link ServiceLoader} and cached statically. The Jackson 2 module provides
 * an implementation that delegates to {@code com.fasterxml.jackson.databind.JsonNode},
 * the Jackson 3 module provides one that delegates to {@code tools.jackson.databind.JsonNode}.
 */
public interface RawJsonValueSupport {

    default int getPriority() {
        return 0;
    }

    RawJsonValue get(RawJsonValue node, String key);

    boolean asBoolean(Object value);

    int asInt(Object value);

    long asLong(Object value);

    String asText(Object value);

    String textValue(Object value);

    boolean isEmpty(Object value);

    String toJsonString(Object value);

    boolean valuesEqual(Object a, Object b);

    int valueHashCode(Object value);

    static RawJsonValueSupport getInstance() {
        return Holder.INSTANCE;
    }

    final class Holder {
        private static final RawJsonValueSupport INSTANCE = load();

        private static RawJsonValueSupport load() {
            RawJsonValueSupport best = null;
            Iterator<RawJsonValueSupport> providers = ServiceLoader.load(RawJsonValueSupport.class).iterator();
            while (providers.hasNext()) {
                try {
                    RawJsonValueSupport candidate = providers.next();
                    if (best == null || candidate.getPriority() > best.getPriority()) {
                        best = candidate;
                    }
                } catch (ServiceConfigurationError e) {
                    // provider's Jackson dependency not on classpath, try next
                }
            }
            if (best == null) {
                throw new IllegalStateException("No RawJsonValueSupport implementation found via ServiceLoader");
            }
            return best;
        }
    }
}
