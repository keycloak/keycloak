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

import org.keycloak.crypto.SignatureSignerContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwtTest {

        @Test
        public void settingsTest() {
                SignatureSignerContext issuerSignerContext = TestSettings.getInstance().getIssuerSignerContext();
                assertNotNull(issuerSignerContext);
        }

        @Test
        public void testA1_Example2_with_nested_disclosure_and_decoy_claims() {
                DisclosureSpec addrDisclosureSpec = DisclosureSpec.builder()
                                .withUndisclosedClaim("street_address", "AJx-095VPrpTtN4QMOqROA")
                                .withUndisclosedClaim("locality", "Pc33JM2LchcU_lHggv_ufQ")
                                .withUndisclosedClaim("region", "G02NSrQfjFXQ7Io09syajA")
                                .withUndisclosedClaim("country", "lklxF5jMYlGTPUovMNIvCA")
                                .withDecoyClaim("2GLC42sKQveCfGfryNRN9w")
                                .withDecoyClaim("eluV5Og3gSNII8EYnsxA_A")
                                .withDecoyClaim("6Ij7tM-a5iVPGboS5tmvVA")
                                .withDecoyClaim("eI8ZWm9QnKPpNPeNenHdhQ")
                                .build();

                DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                                .withUndisclosedClaim("sub", "2GLC42sKQveCfGfryNRN9w")
                                .withUndisclosedClaim("given_name", "eluV5Og3gSNII8EYnsxA_A")
                                .withUndisclosedClaim("family_name", "6Ij7tM-a5iVPGboS5tmvVA")
                                .withUndisclosedClaim("email", "eI8ZWm9QnKPpNPeNenHdhQ")
                                .withUndisclosedClaim("phone_number", "Qg_O64zqAxe412a108iroA")
                                .withUndisclosedClaim("birthdate", "yytVbdAPGcgl2rI4C9GSog")
                                .withDecoyClaim("AJx-095VPrpTtN4QMOqROA")
                                .withDecoyClaim("G02NSrQfjFXQ7Io09syajA")
                                .build();

                // Read claims provided by the holder
                JsonNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/a1.example2-holder-claims.json");

                // Read claims provided by the holder
                JsonNode addressClaimSet = holderClaimSet.get("address");

                // produce the nested sdJwt
                SdJwt addrSdJWT = SdJwt.builder()
                                .withDisclosureSpec(addrDisclosureSpec)
                                .withClaimSet(addressClaimSet)
                                .build();
                JsonNode addPayload = addrSdJWT.asNestedPayload();
                JsonNode expectedAddrPayload = TestUtils.readClaimSet(getClass(),
                                "sdjwt/a1.example2-address-payload.json");
                assertEquals(expectedAddrPayload, addPayload);

                // Verify nested claim has 4 disclosures
                assertEquals(4, addrSdJWT.getDisclosures().size());

                // Set payload back into main claim set
                ((ObjectNode) holderClaimSet).set("address", addPayload);

                // Read claims added by the issuer & merge both
                JsonNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/a1.example2-issuer-claims.json");
                ((ObjectNode) holderClaimSet).setAll((ObjectNode) issuerClaimSet);

                // produce the main sdJwt, adding nested sdJwts
                SdJwt sdJwt = SdJwt.builder()
                                .withDisclosureSpec(disclosureSpec)
                                .withClaimSet(holderClaimSet)
                                .withNestedSdJwt(addrSdJWT)
                                .build();

                IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();
                JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/a1.example2-issuer-payload.json");
                assertEquals(expected, jwt.getPayload());

                // Verify all claims are present.
                // 10 disclosures from 16 digests (6 decoy claims & decoy array elements)
                assertEquals(10, sdJwt.getDisclosures().size());
        }

}
