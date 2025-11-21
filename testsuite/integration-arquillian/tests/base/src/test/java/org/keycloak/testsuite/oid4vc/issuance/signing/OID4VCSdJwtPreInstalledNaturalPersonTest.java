/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.Claim;
import org.keycloak.protocol.oid4vc.model.Format;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.keycloak.models.oid4vci.CredentialScopeModel.CONFIGURATION_ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * OID4VCI testing for the pre-installed oid4vc_natural_person
 *
 */
public class OID4VCSdJwtPreInstalledNaturalPersonTest extends OID4VCIssuerEndpointTest {

    /**
     * This is testing the configuration exposed by OID4VCIssuerWellKnownProvider.
     */
    @Test
    public void testGetSdJwtConfigFromMetadata() {
        final String scopeName = sdJwtTypeNaturalPersonClientScope.getName();
        final String credentialConfigurationId = sdJwtTypeNaturalPersonClientScope.getAttributes().get(CONFIGURATION_ID);
        String expectedIssuer = suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/" + TEST_REALM_NAME;
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    var log = LoggerFactory.getLogger(OID4VCSdJwtPreInstalledNaturalPersonTest.class);
                    var oid4VCIssuerWellKnownProvider = new OID4VCIssuerWellKnownProvider(session);
                    var credentialIssuer = oid4VCIssuerWellKnownProvider.getIssuerMetadata();
                    var credentialsSupported = credentialIssuer.getCredentialsSupported();

                    // Log existing credential configurations
                    credentialsSupported.forEach((k, v) -> {
                        log.info("CredentialsSupported: {}", k);
                    });

                    var jwtVcConfig = credentialsSupported.get(credentialConfigurationId);
                    var credentialBuildConfig = jwtVcConfig.getCredentialBuildConfig();
                    assertEquals(scopeName, jwtVcConfig.getScope());
                    assertEquals(expectedIssuer, credentialBuildConfig.getCredentialIssuer());
                    assertEquals(Format.SD_JWT_VC, jwtVcConfig.getFormat());

                    var credentialMetadata = jwtVcConfig.getCredentialMetadata();
                    var jwtVcClaims = credentialMetadata.getClaims().stream()
                            .collect(Collectors.toMap(Claim::getName, Function.identity()));
                    {
                        Claim claim = jwtVcClaims.get("id");
                        assertEquals("id", claim.getPath().get(0));
                        assertFalse(claim.isMandatory());
                        assertNull(claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get("email");
                        assertEquals("email", claim.getPath().get(0));
                        assertFalse(claim.isMandatory());
                        assertNull(claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get("firstName");
                        assertEquals("firstName", claim.getPath().get(0));
                        assertFalse(claim.isMandatory());
                        assertNull(claim.getDisplay());
                    }
                    {
                        Claim claim = jwtVcClaims.get("familyName");
                        assertEquals("familyName", claim.getPath().get(0));
                        assertFalse(claim.isMandatory());
                        assertNull(claim.getDisplay());
                    }
                }));
    }
}
