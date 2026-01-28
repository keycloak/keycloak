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

import org.keycloak.representations.idm.ClientScopeRepresentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * JWT-specific authorization details flow tests.
 * Extends the base class to inherit common test logic while providing JWT-specific implementations.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class OID4VCJwtAuthorizationDetailsFlowTest extends OID4VCAuthorizationDetailsFlowTestBase {

    @Override
    protected String getCredentialFormat() {
        return "jwt_vc";
    }

    @Override
    protected ClientScopeRepresentation getCredentialClientScope() {
        return jwtTypeCredentialClientScope;
    }

    @Override
    protected String getExpectedClaimPath() {
        return "given_name";
    }

    @Override
    protected void verifyCredentialStructure(Object credentialObj) {
        assertNotNull("Credential object should not be null", credentialObj);

        // For JWT VC, the credential should be a string
        assertTrue("JWT credential should be a string", credentialObj instanceof String);
        String jwtString = (String) credentialObj;
        assertFalse("JWT credential should not be empty", jwtString.isEmpty());

        // Verify it looks like a JWT (contains dots)
        assertTrue("JWT should contain dots", jwtString.contains("."));
    }
}
