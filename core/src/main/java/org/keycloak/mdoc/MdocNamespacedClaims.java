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
package org.keycloak.mdoc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data elements belonging to one ISO mdoc namespace.
 */
public final class MdocNamespacedClaims {

    private final String nameSpace;
    private final Map<String, Object> claims;

    public MdocNamespacedClaims(String nameSpace, Map<String, Object> claims) {
        this.nameSpace = Objects.requireNonNull(nameSpace, "nameSpace");
        this.claims = new LinkedHashMap<>(Objects.requireNonNull(claims, "claims"));
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public Map<String, Object> getClaims() {
        return Collections.unmodifiableMap(claims);
    }

    static MdocNamespacedClaims fromEntry(String nameSpace, Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw new MdocException("The value for the name space '" + nameSpace + "' is not a JSON object.");
        }

        Map<String, Object> claims = new LinkedHashMap<>();
        for (Map.Entry<?, ?> claimEntry : ((Map<?, ?>) value).entrySet()) {
            if (!(claimEntry.getKey() instanceof String)) {
                throw new MdocException("The element identifier for the name space '" + nameSpace + "' is not a string.");
            }
            claims.put((String) claimEntry.getKey(), claimEntry.getValue());
        }
        return new MdocNamespacedClaims(nameSpace, claims);
    }
}
