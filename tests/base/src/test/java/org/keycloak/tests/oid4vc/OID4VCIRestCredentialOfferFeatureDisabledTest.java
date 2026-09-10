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

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = OID4VCIRestCredentialOfferFeatureDisabledTest.CredentialOfferDisabledServerConfig.class)
public class OID4VCIRestCredentialOfferFeatureDisabledTest extends OID4VCIssuerEndpointTest {

    @Test
    public void testRoleNotCreated() {
        assertThrows(NotFoundException.class, () -> testRealm.admin().roles().get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation());
    }

    @Test
    public void testRestEndpoint() {
        String token = getBearerToken(oauth);

        runOnServer.run(session -> {
            BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
            authenticator.setTokenString(token);
            OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

            CorsErrorResponseException exception = assertThrows(CorsErrorResponseException.class, () -> oid4VCIssuerEndpoint.createCredentialOffer("test-credential"));

            // Verify it's a 403 Forbidden
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
        });
    }

    public static class CredentialOfferDisabledServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }

}
