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

package org.keycloak.tests.oid4vc.issuance.credentialbuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_ISSUER;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_HASH_ALGORITHM;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_VCT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class SdJwtCredentialBuilderTest extends CredentialBuilderTest {

    @Test
    public void shouldBuildSdJwtCredentialSuccessfully() throws Exception {
        testSignSDJwtCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                        "test", "test",
                        "arrayClaim", List.of("a", "b", "c")),
                0,
                List.of()
        );
    }

    @Test
    public void buildSdJwtCredential_WithDecoys() throws Exception {
        testSignSDJwtCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                        "test", "test",
                        "arrayClaim", List.of("a", "b", "c")),
                6,
                List.of()
        );
    }

    @Test
    public void buildSdJwtCredential_WithVisibleClaims() throws Exception {
        testSignSDJwtCredential(
                Map.of("id", String.format("uri:uuid:%s", UUID.randomUUID()),
                        "test", "test",
                        "arrayClaim", List.of("a", "b", "c")),
                6,
                List.of("test")
        );
    }

    @Test
    public void buildSdJwtCredential_WithNoClaims() throws Exception {
        testSignSDJwtCredential(
                Map.of(),
                0,
                List.of()
        );
    }

    public void testSignSDJwtCredential(Map<String, Object> claims, int decoys, List<String> visibleClaims)
            throws VerificationException {
        String issuerDid = TEST_DID.toString();
        CredentialBuildConfig credentialBuildConfig = new CredentialBuildConfig()
                .setCredentialIssuer(issuerDid)
                .setCredentialType("https://credentials.example.com/test-credential")
                .setTokenJwsType("example+sd-jwt")
                .setHashAlgorithm(OID4VCConstants.SD_HASH_DEFAULT_ALGORITHM)
                .setNumberOfDecoys(decoys)
                .setSdJwtVisibleClaims(visibleClaims);

        VerifiableCredential testCredential = getTestCredential(claims);
        SdJwtCredentialBody sdJwtCredentialBody = new SdJwtCredentialBuilder()
                .buildCredentialBody(testCredential, credentialBuildConfig);

        String sdJwtString = sdJwtCredentialBody.sign(exampleSigner());
        SdJwtVP sdJwt = SdJwtVP.of(sdJwtString);

        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();

        assertEquals(issuerDid,
                jwt.getPayload().get(CLAIM_NAME_ISSUER).asText(),
                "The issuer should be set in the token.");

        assertEquals(credentialBuildConfig.getCredentialType(),
                jwt.getPayload().get(CLAIM_NAME_VCT).asText(),
                "The type should be included");

        assertEquals(credentialBuildConfig.getTokenJwsType(),
                jwt.getJwsHeader().getType(),
                "The JWS token type should be included");

        ArrayNode sdArrayNode = (ArrayNode) jwt.getPayload().get(CLAIM_NAME_SD);
        if (sdArrayNode != null) {
            assertEquals(credentialBuildConfig.getHashAlgorithm().toLowerCase(),
                    jwt.getPayload().get(CLAIM_NAME_SD_HASH_ALGORITHM).asText(),
                    "The algorithm should be included and lowercase");
        }

        List<String> disclosed = sdJwt.getDisclosures().values().stream().toList();
        assertEquals(disclosed.size() + (decoys == 0 ? SdJwt.DEFAULT_NUMBER_OF_DECOYS : decoys),
                sdArrayNode == null ? 0 : sdArrayNode.size(),
                "All undisclosed claims and decoys should be provided.");

        visibleClaims.forEach(vc ->
                assertTrue(jwt.getPayload().has(vc),
                        "The visible claims should be present within the token.")
        );

        sdJwt.getSdJwtVerificationContext()
                .verifyIssuance(List.of(exampleVerifier()),
                        IssuerSignedJwtVerificationOpts.builder()
                                .withIatCheck(true)
                                .withNbfCheck(true)
                                .withExpCheck(true)
                                .build(),
                        null);
    }
}
