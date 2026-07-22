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

package org.keycloak.tests.oid4vc.feature;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;

import org.junit.jupiter.api.Test;

import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCMinimalServerConfig.class)
public class OID4VCIRestCredentialOfferFeatureDisabledTest extends OID4VCIssuerTestBase {

    @Test
    public void testRoleNotCreated() {
        assertThrows(NotFoundException.class, () -> testRealm.admin().roles().get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation());
    }

    @Test
    public void testRestEndpoint() {

        var ctx = new OID4VCTestContext(client, jwtTypeCredentialScope);

        AccessTokenResponse tokenResponse = wallet.getAccessToken(ctx.getIssuer(), ctx.getScope());
        String issToken = tokenResponse.getAccessToken();

        String credConfigId = ctx.getCredentialConfigurationId();
        CredentialOfferUriResponse uriResponse = wallet.credentialOfferUriRequest(ctx, credConfigId)
                .bearerToken(issToken)
                .send();

        assertFalse(uriResponse.isSuccess(), "Expected to fail");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), uriResponse.getStatusCode());
        assertEquals("REST credential offer functionality is not enabled", uriResponse.getErrorDescription());
    }
}
