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

import org.keycloak.protocol.oidc.grants.ciba.channel.CIBAAuthenticationRequest;
import org.keycloak.representations.JsonWebToken;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the serialize/deserialize round-trip detection of forged {@code otherClaims} in a
 * {@link CIBAAuthenticationRequest}, as used by {@code BackchannelAuthenticationEndpoint}.
 */
public class CibaOtherClaimsValidatorTest {

    @Test
    public void noOtherClaimsIsNotForged() {
        CIBAAuthenticationRequest request = new CIBAAuthenticationRequest();
        request.subject("real-subject");

        assertFalse(CibaOtherClaimsValidator.hasForgedOtherClaims(request));
    }

    @Test
    public void customClaimSurvivesRoundTrip() {
        CIBAAuthenticationRequest request = new CIBAAuthenticationRequest();
        request.subject("real-subject");
        // A Long whose JSON representation is deserialized as an Integer: the value type changes but
        // the key must still be present, so this must not be treated as a forged claim.
        request.setOtherClaims("custom", 1L);

        assertFalse(CibaOtherClaimsValidator.hasForgedOtherClaims(request));
    }

    @Test
    public void nullCustomClaimIsNotForged() {
        CIBAAuthenticationRequest request = new CIBAAuthenticationRequest();
        request.subject("real-subject");
        // Serialization omits null values, so a null custom claim disappears from otherClaims after the
        // round trip without colliding with any reserved property; it must not be treated as forged.
        request.setOtherClaims("custom", null);

        assertFalse(CibaOtherClaimsValidator.hasForgedOtherClaims(request));
    }

    @Test
    public void claimCollidingWithInitializedReservedPropertyIsForged() {
        CIBAAuthenticationRequest request = new CIBAAuthenticationRequest();
        request.subject("real-subject");
        // "sub" collides with the server-initialized subject property; it is consumed on deserialization
        // and disappears from otherClaims.
        request.setOtherClaims(JsonWebToken.SUBJECT, "forged-subject");

        assertTrue(CibaOtherClaimsValidator.hasForgedOtherClaims(request));
    }

    @Test
    public void claimCollidingWithUnsetReservedPropertyIsForged() {
        CIBAAuthenticationRequest request = new CIBAAuthenticationRequest();
        request.subject("real-subject");
        // "typ" collides with an initially unset reserved property; it is consumed on deserialization
        // and disappears from otherClaims.
        request.setOtherClaims(JsonWebToken.TYPE, "forged-type");

        assertTrue(CibaOtherClaimsValidator.hasForgedOtherClaims(request));
    }
}
