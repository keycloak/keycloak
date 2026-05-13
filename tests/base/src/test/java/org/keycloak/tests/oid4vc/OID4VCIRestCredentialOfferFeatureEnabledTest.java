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

package org.keycloak.tests.oid4vc;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfigRestCredentialOffer.class)
public class OID4VCIRestCredentialOfferFeatureEnabledTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testRoleCreated() {
        assertEquals(CREDENTIAL_OFFER_CREATE.getName(), testRealm.admin().roles().get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation().getName());
    }

    @Test
    public void testRestEndpoint() {
        String scopeName = jwtTypeCredentialScope.getName();
        String credentialConfigurationId = jwtTypeCredentialScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        String token = getBearerToken(oauth, client, scopeName);

        runOnServer.run(session -> {
                BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
                authenticator.setTokenString(token);
                OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                Response response = oid4VCIssuerEndpoint.createCredentialOffer(credentialConfigurationId);

                assertEquals(HttpStatus.SC_OK, response.getStatus());

                CredentialOfferURI credentialOfferURI = JsonSerialization.mapper.convertValue(response.getEntity(), CredentialOfferURI.class);

                assertNotNull(credentialOfferURI.getNonce());
                assertNotNull(credentialOfferURI.getIssuer());
        });
    }
}
