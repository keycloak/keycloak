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
package org.keycloak.tests.oid4vc;

import jakarta.ws.rs.core.Response;

import org.keycloak.VCFormat;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCMdocFeatureDisabledWellKnownProviderTest extends OID4VCIssuerTestBase {

    @Test
    void testMdocCredentialConfigurationRejectedWhenFeatureDisabled() {
        CredentialScopeRepresentation mdocScope = new CredentialScopeRepresentation("mdoc-feature-off-scope")
                .setCredentialConfigurationId("mdoc-feature-off-config")
                .setFormat(VCFormat.MSO_MDOC);

        try (Response response = testRealm.admin().clientScopes().create(mdocScope)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            String error = response.readEntity(String.class);
            assertTrue(error.contains(VCFormat.MSO_MDOC), error);
            assertTrue(error.contains("No credential builder found"), error);
        }
    }

    @Test
    void testNaturalPersonMdocScopeNotAutoCreatedWhenFeatureDisabled() {
        assertNull(getCredentialScope(mdocTypeNaturalPersonScopeName),
                "the natural person mdoc scope must only be auto created when the mDoc feature is enabled");
    }
}
