/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class IssuerSignedJWTTest {
    /**
     * If issuer decides to disclose everything, paylod of issuer signed JWT should
     * be same as the claim set.
     *
     * This is essential for backward compatibility with non sd based jwt issuance.
     *
     * @throws IOException
     */
    @Test
    public void testIssuerSignedJWTPayloadWithValidClaims() {
        ObjectNode claimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s6.1-holder-claims.json");

        List<SdJwtClaim> claims = new ArrayList<>();
        claimSet.properties().forEach(entry -> {
            claims.add(
                    VisibleSdJwtClaim.builder().withClaimName(entry.getKey()).withClaimValue(entry.getValue()).build());
        });

        IssuerSignedJWT jwt = IssuerSignedJWT.builder().withClaims(claims).build();

        assertEquals(claimSet, jwt.getPayload());
    }

    @Test
    public void testIssuerSignedJWTPayloadThrowsExceptionForDuplicateClaims() throws IOException {
        ObjectNode claimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s6.1-holder-claims.json");

        List<SdJwtClaim> claims = new ArrayList<>();

        // First fill claims
        claimSet.properties().forEach(entry -> {
            claims.add(
                    VisibleSdJwtClaim.builder().withClaimName(entry.getKey()).withClaimValue(entry.getValue()).build());
        });

        // First fill claims
        claimSet.properties().forEach(entry -> {
            claims.add(
                    VisibleSdJwtClaim.builder().withClaimName(entry.getKey()).withClaimValue(entry.getValue()).build());
        });

        // All claims are duplicate.
        assertEquals(claims.size(), claimSet.size() * 2);

        // Expecting exception
        assertThrows(IllegalArgumentException.class, () -> IssuerSignedJWT.builder().withClaims(claims).build());
    }

    @Test
    public void testIssuerSignedJWTWithUndiclosedClaims6_1() {
        ObjectNode claimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s6.1-holder-claims.json");

        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("email", "JnwGqRFZjMprsoZobherdQ")
                .withUndisclosedClaim("phone_number", "ffZ03jm_zeHyG4-yoNt6vg")
                .withUndisclosedClaim("address", "INhOGJnu82BAtsOwiCJc_A")
                .withUndisclosedClaim("birthdate", "d0l3jsh5sBzj2oEhZxrJGw").build();

        SdJwt sdJwt = SdJwt.builder()
                           .withIssuerSignedJwt(IssuerSignedJWT.builder().withClaims(claimSet, disclosureSpec).build())
                           .withUseDefaultDecoys(false)
                           .build();

        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();

        JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/s6.1-issuer-payload.json");
        assertEquals(expected, jwt.getPayload());
    }

    @Test
    public void testIssuerSignedJWTWithUndiclosedClaims3_3() {
        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedClaim("family_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("email", "6Ij7tM-a5iVPGboS5tmvVA")
                .withUndisclosedClaim("phone_number", "eI8ZWm9QnKPpNPeNenHdhQ")
                .withUndisclosedClaim("address", "Qg_O64zqAxe412a108iroA")
                .withUndisclosedClaim("birthdate", "AJx-095VPrpTtN4QMOqROA")
                .withUndisclosedClaim("is_over_18", "Pc33JM2LchcU_lHggv_ufQ")
                .withUndisclosedClaim("is_over_21", "G02NSrQfjFXQ7Io09syajA")
                .withUndisclosedClaim("is_over_65", "lklxF5jMYlGTPUovMNIvCA")
                .build();

        // Read claims provided by the holder
        ObjectNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-holder-claims.json");
        // Read claims added by the issuer
        ObjectNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-issuer-claims.json");

        // Merge both
        holderClaimSet.setAll(issuerClaimSet);

        SdJwt sdJwt = SdJwt.builder()
                .withIssuerSignedJwt(IssuerSignedJWT.builder().withClaims(holderClaimSet, disclosureSpec).build())
                .withUseDefaultDecoys(false)
                .build();
        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();

        JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-issuer-payload.json");
        assertEquals(expected, jwt.getPayload());
    }
}
