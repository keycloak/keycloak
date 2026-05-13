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
package org.keycloak.sdjwt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Result of successful SD-JWT verification, including the fully disclosed payload.
 */
public class VerifiedSdJwt {

    private final ObjectNode disclosedPayload;

    VerifiedSdJwt(ObjectNode disclosedPayload) {
        this.disclosedPayload = disclosedPayload.deepCopy();
    }

    public Optional<String> getStringClaim(String claimName) {
        JsonNode claim = disclosedPayload.get(claimName);
        return claim != null && claim.isTextual() ? Optional.of(claim.textValue()) : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getClaims() {
        return JsonSerialization.mapper.convertValue(disclosedPayload, LinkedHashMap.class);
    }

}
