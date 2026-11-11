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
package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import java.io.IOException;
import java.util.Map;

import org.keycloak.protocol.oidc.grants.ciba.channel.CIBAAuthenticationRequest;
import org.keycloak.util.JsonSerialization;

/**
 * Detects forged claims smuggled into the {@link CIBAAuthenticationRequest#getOtherClaims() otherClaims}
 * of a parsed CIBA authentication request.
 */
public final class CibaOtherClaimsValidator {

    private CibaOtherClaimsValidator() {
    }

    /**
     * Checks whether a serialize/deserialize round trip drops any {@code otherClaims} entry because its
     * name collides with an internal/reserved request property.
     * <p>
     * A claim whose name collides with a reserved property is consumed into that property during
     * deserialization and disappears from {@code otherClaims}. Claim values may legitimately change their
     * Java type during the round trip (for example a {@code Long} becoming an {@code Integer}), so values
     * are never compared. A claim with a {@code null} value is also dropped, but only because serialization
     * omits {@code null} values, which is not a collision, so such entries are ignored.
     *
     * @param request the parsed authentication request
     * @return {@code true} if a non-{@code null} {@code otherClaims} entry disappears after the round trip,
     *         i.e. a forged claim collided with a reserved property; {@code false} otherwise
     */
    public static boolean hasForgedOtherClaims(CIBAAuthenticationRequest request) {
        try {
            String serialized = JsonSerialization.writeValueAsString(request);
            CIBAAuthenticationRequest deserialized = JsonSerialization.readValue(serialized, CIBAAuthenticationRequest.class);
            Map<String, Object> roundTrippedClaims = deserialized.getOtherClaims();
            for (Map.Entry<String, Object> claim : request.getOtherClaims().entrySet()) {
                if (claim.getValue() != null && !roundTrippedClaims.containsKey(claim.getKey())) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to validate CIBA authentication request serialization.", e);
        }
    }
}
