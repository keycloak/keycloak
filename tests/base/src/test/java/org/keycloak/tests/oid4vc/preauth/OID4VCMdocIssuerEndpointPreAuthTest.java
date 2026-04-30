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
package org.keycloak.tests.oid4vc.preauth;

import java.util.Map;

import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.oid4vc.OID4VCMdocTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCMdocTestBase.VCTestServerWithPreAuthCodeAndMdocEnabled.class)
public class OID4VCMdocIssuerEndpointPreAuthTest extends OID4VCMdocTestBase {

    private CredentialScopeRepresentation mdocScope;

    @BeforeEach
    void setUpMdocScope() {
        ensureEcSigningKeyProvider("mdoc-preauth-endpoint-issuer-key", "P-256", "ES256", 200);
        mdocScope = createMdocCredentialScope("mdoc-preauth-endpoint-scope", "mdoc-preauth-endpoint-config");
    }

    @AfterEach
    public void logout() {
        AccountHelper.logout(testRealm.admin(), "john");
    }

    @Test
    public void testCredentialIssuancePreAuthForMdoc() {
        String token = oauth.openid(false)
                .scope(mdocScope.getName())
                .doPasswordGrantRequest("john", TEST_PASSWORD)
                .getAccessToken();
        assertNotNull(token);

        String credentialConfigurationId = mdocScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialOfferURI credOfferUri = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .targetUser("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();
        assertNotNull(credOfferUri);

        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().doCredentialOfferRequest(credOfferUri);
        CredentialsOffer credentialsOffer = credentialOfferResponse.getCredentialsOffer();
        assertNotNull(credentialsOffer);

        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(credentialsOffer.getIssuerMetadataUrl())
                .send()
                .getMetadata();
        OIDCConfigurationRepresentation openidConfig = oauth.wellknownRequest()
                .url(credentialIssuer.getAuthorizationServers().get(0))
                .send()
                .getOidcConfiguration();

        assertTrue(openidConfig.getGrantTypesSupported().contains(PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE));

        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(credentialsOffer.getPreAuthorizedCode())
                .endpoint(openidConfig.getTokenEndpoint())
                .send();
        assertEquals(200, accessTokenResponse.getStatusCode());
        assertFalse(accessTokenResponse.getOID4VCAuthorizationDetails().isEmpty());

        String credentialIdentifier = accessTokenResponse.getOID4VCAuthorizationDetails().get(0).getCredentialIdentifiers().get(0);
        OID4VCTestContext ctx = new OID4VCTestContext(client, mdocScope);
        CredentialResponse credentialResponse = oauth.oid4vc()
                .credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .proofs(wallet.generateJwtProof(ctx))
                .bearerToken(accessTokenResponse.getAccessToken())
                .send()
                .getCredentialResponse();

        assertValidMdocCredential(credentialResponse);
    }

    private void assertValidMdocCredential(CredentialResponse credentialResponse) {
        assertNotNull(credentialResponse);
        assertNotNull(credentialResponse.getCredentials());
        assertEquals(1, credentialResponse.getCredentials().size());

        Object credential = credentialResponse.getCredentials().get(0).getCredential();
        assertInstanceOf(String.class, credential, "mDoc credential should be a string");

        assertDoesNotThrow(() -> {
            Map<String, Object> nameSpaces = getMdocNamespaces((String) credential);
            assertTrue(nameSpaces.containsKey("org.iso.18013.5.1"));

            Map<String, Object> mobileSecurityObject = getMdocMobileSecurityObject((String) credential);
            assertEquals(mdocTypeCredentialDocType, mobileSecurityObject.get("docType"));
            assertNotNull(mobileSecurityObject.get("deviceKeyInfo"));
        });
    }
}
