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

package org.keycloak.tests.oid4vc.issuance.signing;

import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCAuthorizationDetailsFlowTestBase;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT-specific authorization details flow tests.
 * Extends the base class to inherit common test logic while providing JWT-specific implementations.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCJwtAuthorizationDetailsFlowTest extends OID4VCAuthorizationDetailsFlowTestBase {

    @Override
    protected String getCredentialFormat() {
        return "jwt_vc";
    }

    @Override
    protected ClientScopeRepresentation getCredentialClientScope() {
        return jwtTypeCredentialScope;
    }

    @Override
    protected String getExpectedClaimPath() {
        return "given_name";
    }

    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull(credentialObj, "Credential object should not be null");

        // For JWT VC, the credential should be a string
        assertTrue(credentialObj instanceof String, "JWT credential should be a string");
        String jwtString = (String) credentialObj;
        assertFalse(jwtString.isEmpty(), "JWT credential should not be empty");

        // Verify it looks like a JWT (contains dots)
        assertTrue(jwtString.contains("."), "JWT should contain dots");
    }
}
