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

import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsProcessor;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.junit.Before;
import org.junit.Test;

import static org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsProcessor.OPENID_CREDENTIAL_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify that authorization_details_types_supported is included in the OAuth Authorization Server
 * metadata endpoint (/.well-known/oauth-authorization-server/) and that credential issuance works with
 * authorization_details when scope is absent.
 */
@EnableFeature(value = Profile.Feature.OID4VC_VCI, skipRestart = true)
public class OID4VCAuthorizationDetailsTypesSupportedTest extends OID4VCIssuerEndpointTest {

    @Before
    public void enableOpenID4VC() {
        enableOpenID4VC(true);
    }

    public void enableOpenID4VC(boolean enabled) {
        RealmRepresentation realmRep = adminClient.realm(TEST_REALM_NAME).toRepresentation();
        realmRep.setVerifiableCredentialsEnabled(enabled);
        adminClient.realm(TEST_REALM_NAME).update(realmRep);
    }

    @Test
    public void testAuthorizationDetailsTypesSupportedInOAuthAuthorizationServerMetadata() {
        try (Client client = AdminClientUtil.createResteasyClient()) {
            // Get OAuth Authorization Server metadata as required by OID4VC spec
            OIDCConfigurationRepresentation oauthConfig = getOAuth2WellKnownConfiguration(client);

            // Verify that authorization_details_types_supported is present
            assertNotNull("authorization_details_types_supported should be present",
                    oauthConfig.getAuthorizationDetailsTypesSupported());

            // Verify that it contains openid_credential
            List<String> supportedTypes = oauthConfig.getAuthorizationDetailsTypesSupported();
            assertTrue("authorization_details_types_supported should contain openid_credential",
                    supportedTypes.contains(OID4VCAuthorizationDetailsProcessor.OPENID_CREDENTIAL_TYPE));

        }
    }

    @Test
    public void testCredentialIssuerAuthorizationServerMetadata() {
        try (Client client = AdminClientUtil.createResteasyClient()) {
            // Get credential issuer metadata
            CredentialIssuer credentialIssuer = getCredentialIssuerMetadata();
            assertNotNull("Credential issuer should not be null", credentialIssuer);
            assertNotNull("Authorization servers should be present", credentialIssuer.getAuthorizationServers());
            assertFalse("Authorization servers should not be empty", credentialIssuer.getAuthorizationServers().isEmpty());

            // Verify the authorization server from credential issuer metadata
            String authServerUri = credentialIssuer.getAuthorizationServers().get(0);
            OIDCConfigurationRepresentation authServerConfig = getOAuth2WellKnownConfiguration(client, authServerUri + "/.well-known/oauth-authorization-server");
            assertNotNull("Authorization server should support authorization_details_types_supported",
                    authServerConfig.getAuthorizationDetailsTypesSupported());
            assertTrue("Authorization server should support openid_credential",
                    authServerConfig.getAuthorizationDetailsTypesSupported().contains(OPENID_CREDENTIAL_TYPE));
        }
    }

    @Test
    public void testAuthorizationDetailsTypesSupportedNotInOAuth2WellKnownWhenOID4VCDisabled() {
        // Disable OID4VC for this realm
        enableOpenID4VC(false);

        try (Client client = AdminClientUtil.createResteasyClient()) {
            // Get OAuth2 well-known configuration
            OIDCConfigurationRepresentation oauth2Config = getOAuth2WellKnownConfiguration(client);

            // Verify that authorization_details_types_supported is not present
            assertNull("authorization_details_types_supported should not be present when OID4VC is disabled",
                    oauth2Config.getAuthorizationDetailsTypesSupported());

        }
    }

    private OIDCConfigurationRepresentation getOAuth2WellKnownConfiguration(Client client) {
        return getOAuth2WellKnownConfiguration(client, OAuthClient.AUTH_SERVER_ROOT + "/.well-known/oauth-authorization-server/realms/test");
    }

    private OIDCConfigurationRepresentation getOAuth2WellKnownConfiguration(Client client, String oauth2WellKnownUri) {
        Response response = client.target(oauth2WellKnownUri)
                .request()
                .get();

        assertEquals("OAuth Authorization Server metadata endpoint should return 200", 200, response.getStatus());

        return response.readEntity(OIDCConfigurationRepresentation.class);
    }
}
