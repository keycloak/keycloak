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
package org.keycloak.tests.oid4vc.preauth;

import java.util.List;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerEndpointTest;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.jwtProofs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithPreAuthCodeEnabled.class)
public class OID4VCJWTIssuerEndpointPreAuthTest extends OID4VCIssuerEndpointTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @AfterEach
    public void logout() {
        AccountHelper.logout(testRealm.admin(), "john");
    }

    @Test
    public void testCredentialIssuancePreAuth() {
        String token = getBearerToken(oauth, client, jwtTypeCredentialScope.getName());

        // 1. Retrieving the credential-offer-uri
        final String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);

        CredentialOfferURI credOfferUri = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .targetUser("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();

        assertNotNull(credOfferUri, "A valid offer uri should be returned");

        // 2. Using the uri to get the actual credential offer
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc()
                .doCredentialOfferRequest(credOfferUri);
        CredentialsOffer credentialsOffer = credentialOfferResponse.getCredentialsOffer();

        assertNotNull(credentialsOffer, "A valid offer should be returned");

        // 3. Get the issuer metadata
        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(credentialsOffer.getIssuerMetadataUrl())
                .send()
                .getMetadata();

        assertNotNull(credentialIssuer, "Issuer metadata should be returned");
        assertEquals(1, credentialIssuer.getAuthorizationServers().size(), "We only expect one authorization server.");

        // 4. Get the openid-configuration
        OIDCConfigurationRepresentation openidConfig = oauth.wellknownRequest()
                .url(credentialIssuer.getAuthorizationServers().get(0))
                .send()
                .getOidcConfiguration();

        assertNotNull(openidConfig.getTokenEndpoint(), "A token endpoint should be included.");
        assertTrue(openidConfig.getGrantTypesSupported().contains(PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE), "The pre-authorized code should be supported.");

        // 5. Get an access token for the pre-authorized code
        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(credentialsOffer.getPreAuthorizedCode())
                .endpoint(openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String theToken = accessTokenResponse.getAccessToken();
        assertNotNull(theToken, "Access token should be present");

        // Extract credential_identifier from authorization_details in token response
        List<OID4VCAuthorizationDetail> authDetailsResponse = accessTokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertFalse(authDetailsResponse.isEmpty(), "authorization_details should not be empty");

        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "Credential identifier should be present");

        // 6. Get the credential using credential_identifier (required when authorization_details are present)
        credentialsOffer.getCredentialConfigurationIds().stream()
                .map(offeredCredentialId -> credentialIssuer.getCredentialsSupported().get(offeredCredentialId))
                .map(credConfigId -> credentialIssuer.getCredentialsSupported().get(credConfigId))
                .forEach(supportedCredential -> {
                    try {
                        String cNonce = oauth.oid4vc().nonceRequest().send().getNonce();
                        Proofs proofs = jwtProofs(credentialIssuer.getCredentialIssuer(), cNonce);

                        CredentialResponse credentialResponse = oauth.oid4vc()
                                .credentialRequest()
                                .bearerToken(theToken)
                                .credentialIdentifier(credentialIdentifier)
                                .proofs(proofs)
                                .send()
                                .getCredentialResponse();

                        JsonWebToken jsonWebToken = TokenVerifier.create(
                                (String) credentialResponse.getCredentials().get(0).getCredential(),
                                JsonWebToken.class
                        ).getToken();
                        assertNotNull(jsonWebToken);
                    } catch (VerificationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testPreAuthorizedCodeValidAfterOfferConsumed() {

        String token = getBearerToken(oauth, client, jwtTypeCredentialScope.getName());
        final String credentialConfigurationId = jwtTypeCredentialScope.getAttributes()
                .get(CredentialScopeModel.VC_CONFIGURATION_ID);

        // 1. Fetch the Offer URI
        CredentialOfferURI credentialOfferURI = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .targetUser("john")
                .bearerToken(token)
                .send()
                .getCredentialOfferURI();

        assertNotNull(credentialOfferURI, "Credential offer URI should not be null");
        String nonce = credentialOfferURI.getNonce();
        assertNotNull(nonce, "Nonce should not be null");

        // 2. Fetch the Offer JSON (this removes the nonce entry for replay protection)
        CredentialsOffer credentialsOffer = oauth.oid4vc()
                .credentialOfferRequest(nonce)
                .bearerToken(token)
                .send()
                .getCredentialsOffer();

        assertNotNull(credentialsOffer, "Credential offer should not be null");

        String preAuthorizedCode = credentialsOffer.getPreAuthorizedCode();
        assertNotNull(preAuthorizedCode, "Pre-authorized code value should not be null");

        // 3. Immediately perform the Token Request (Pre-Authorized Code Grant) using the valid code
        CredentialIssuer credentialIssuer = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(credentialsOffer.getIssuerMetadataUrl())
                .send()
                .getMetadata();

        OIDCConfigurationRepresentation openidConfig = oauth.wellknownRequest()
                .url(credentialIssuer.getAuthorizationServers().get(0))
                .send()
                .getOidcConfiguration();

        assertNotNull(openidConfig.getTokenEndpoint(), "Token endpoint should be present");

        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(preAuthorizedCode)
                .send();

        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode(), "Token request should succeed even after nonce is removed for replay protection");
        assertNotNull(accessTokenResponse.getAccessToken(), "Access token should be present");
        assertFalse(accessTokenResponse.getAccessToken().isEmpty(), "Access token should not be empty");
    }
}
