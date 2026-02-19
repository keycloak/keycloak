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
package org.keycloak.testsuite.oid4vc.issuance;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.directory.api.util.Strings;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.SCOPE_OPENID;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Credential Offer Validity Matrix
 * <p>
 * +----------+----------+---------+------------------------------------------------------+
 * | Pre-Auth | Username | Valid   | Notes                                                |
 * +----------+----------+---------+------------------------------------------------------+
 * | no       | no       | yes     | Anonymous offer; any logged-in user may redeem.      |
 * | no       | yes      | yes     | Offer restricted to a specific user.                 |
 * +----------+----------+---------+------------------------------------------------------+
 * | yes      | no       | no      | Pre-auth requires a target user.                     |
 * | yes      | yes      | yes     | Pre-auth for a specific target user.                 |
 * +----------+----------+---------+------------------------------------------------------+
 */
public class OID4VCICredentialOfferMatrixTest extends OID4VCIssuerEndpointTest {

    String namedClientId = "named-test-app";

    String issUsername = "john";

    String appUsername = "alice";

    String credScopeName = jwtTypeNaturalPersonScopeName;
    String credConfigId = jwtTypeNaturalPersonScopeName;

    class TestContext {
        boolean preAuthorized;
        String issUser;
        String appUser;
        CredentialIssuer issuerMetadata;
        OIDCConfigurationRepresentation authorizationMetadata;
        SupportedCredentialConfiguration credentialConfiguration;

        TestContext(boolean preAuth, String appUser) {
            this.preAuthorized = preAuth;
            this.issUser = issUsername;
            this.appUser = appUser;
            this.issuerMetadata = getCredentialIssuerMetadata();
            this.authorizationMetadata = getAuthorizationMetadata(this.issuerMetadata.getAuthorizationServers().get(0));
            this.credentialConfiguration = this.issuerMetadata.getCredentialsSupported().get(credConfigId);
        }
    }

    @Override
    protected void afterAbstractKeycloakTestRealmImport() {
        ClientRepresentation namedClient = requireExistingClient(namedClientId);
        assignOptionalClientScope(namedClient, credScopeName);
        setOid4vciEnabled(namedClient, true);
    }

    @Test
    public void testCredentialWithoutOffer() throws Exception {
        var ctx = new TestContext(false, appUsername);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(ctx.issuerMetadata.getCredentialIssuer()));

        // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
        // https://github.com/keycloak/keycloak/issues/44320
        String accessToken = getBearerToken(clientId, ctx.appUser, credScopeName, authDetail);

        // Extract credential_identifier from the access token's authorization_details
        JsonWebToken tokenDecoded = new JWSInput(accessToken).readJsonContent(JsonWebToken.class);
        Object tokenAuthDetails = tokenDecoded.getOtherClaims().get(OAuth2Constants.AUTHORIZATION_DETAILS);
        assertNotNull("authorization_details not found in access token", tokenAuthDetails);

        // When authorization_details are sent in token request, they are returned in token response with credential_identifiers
        // The credential request MUST use credential_identifier (not credential_configuration_id)
        List<OID4VCAuthorizationDetail> authDetailsResponse = JsonSerialization.readValue(
                JsonSerialization.writeValueAsString(tokenAuthDetails),
                new TypeReference<>() {}
        );
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertFalse("authorization_details should not be empty", authDetailsResponse.isEmpty());

        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        List<String> credentialIdentifiers = authDetailResponse.getCredentialIdentifiers();
        assertNotNull("credential_identifiers should be present", credentialIdentifiers);
        assertFalse("credential_identifiers should not be empty", credentialIdentifiers.isEmpty());

        var credRequest = new CredentialRequest()
                .setCredentialIdentifier(credentialIdentifiers.get(0));

        CredentialResponse credResponse = sendCredentialRequest(accessToken, credRequest);
        verifyCredentialResponse(ctx, credResponse);
    }

    // Pre Authorized --------------------------------------------------------------------------------------------------

    @Test
    public void testCredentialOffer_PreAuth_SelfIssued() throws Exception {
        runCredentialOfferTest(new TestContext(true, issUsername));
    }

    @Test
    public void testCredentialOffer_PreAuth_Targeted() throws Exception {
        runCredentialOfferTest(new TestContext(true, appUsername));
    }

    @Test
    public void testCredentialOffer_PreAuth_DisabledUser() throws Exception {
        // Disable user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), appUsername);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEnabled(false);
        user.update(userRep);

        try {
            runCredentialOfferTest(new TestContext(true, appUsername));
            fail("Expected " + INVALID_CREDENTIAL_OFFER_REQUEST.name());
        } catch (RuntimeException ex) {
            List.of(INVALID_CREDENTIAL_OFFER_REQUEST.name(), "User '" + appUsername + "' disabled")
                    .forEach(it -> assertTrue(ex.getMessage() + " does not contain " + it, ex.getMessage().contains(it)));
        } finally {
            // Re-enable user
            userRep.setEnabled(true);
            user.update(userRep);
        }
    }

    // Authorization Code ----------------------------------------------------------------------------------------------

    @Test
    public void testCredentialOffer_noPreAuth_Anonymous() throws Exception {
        runCredentialOfferTest(new TestContext(false, null));
    }

    @Test
    public void testCredentialOffer_noPreAuth_Targeted() throws Exception {
        runCredentialOfferTest(new TestContext(false, appUsername));
    }

    void runCredentialOfferTest(TestContext ctx) throws Exception {

        // Issuer login
        //
        String issToken = getBearerToken(clientId, ctx.issUser, SCOPE_OPENID);

        // Exclude scope: <credScope>
        // Require role: credential-offer-create
        // [TODO] Require role: credential-offer-create
        verifyTokenJwt(issToken,
                List.of(), List.of(ctx.credentialConfiguration.getScope()),
                List.of(), List.of());

        // Retrieving the credential-offer-uri
        //
        CredentialOfferURI credOfferUri = getCredentialOfferUri(ctx, issToken);

        // Issuer logout in order to remove unwanted session state
        //
        logout(ctx.issUser);

        try {

            // Using the uri to get the actual credential offer
            //
            CredentialsOffer credOffer = getCredentialsOffer(ctx, credOfferUri);

            if (credOffer.getCredentialConfigurationIds().size() > 1)
                throw new IllegalStateException("Multiple credential configuration ids not supported in: " + JsonSerialization.valueAsString(credOffer));

            if (ctx.preAuthorized) {

                // Get an access token for the pre-authorized code (PAC)
                //
                // For a PAC access token, we treat all scopes and all roles as non-meaningful.
                // The access token:
                //  1. has no authenticated user, and therefore cannot carry any user roles
                //  2. does not perform authorization-based scope filtering
                //  3. does not derive scopes from the client configuration
                //  4. does not reflect anything from the credential offer
                //
                AccessTokenResponse accessToken = getPreAuthorizedAccessTokenResponse(credOffer);
                assertTrue(accessToken.getErrorDescription(), accessToken.isSuccess());

                List<OID4VCAuthorizationDetail> authDetailsResponse = accessToken.getOid4vcAuthorizationDetails();
                if (authDetailsResponse == null || authDetailsResponse.isEmpty()) {
                    throw new IllegalStateException("No authorization_details in token response");
                }
                if (authDetailsResponse.size() > 1) {
                    throw new IllegalStateException("Multiple authorization_details in token response");
                }
                OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);

                // Get the credential and verify
                //
                CredentialResponse credResponse = getCredentialByAuthDetail(accessToken.getAccessToken(), authDetailResponse);
                verifyCredentialResponse(ctx, credResponse);

            } else {

                String username = ctx.appUser != null ? ctx.appUser : appUsername;
                String credConfigId = credOffer.getCredentialConfigurationIds().get(0);

                SupportedCredentialConfiguration credConfig = ctx.issuerMetadata.getCredentialsSupported().get(credConfigId);
                String scope = credConfig.getScope();

                AccessTokenResponse tokenResponse = getBearerTokenResponse(clientId, username, scope);
                String accessToken = tokenResponse.getAccessToken();

                // Get the credential and verify
                //
                CredentialResponse credResponse = getCredentialByOffer(accessToken, tokenResponse, credOffer);
                verifyCredentialResponse(ctx, credResponse);
            }
        } finally {
            if (ctx.appUser != null) {
                logout(ctx.appUser);
            }
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private AccessTokenResponse getBearerTokenResponse(String clientId, String username, String scope) {
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);

        // For credential scopes, we need to request authorization_details to get credential_identifier
        if (scope != null && scope.equals(credScopeName)) {
            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credConfigId);
            authDetail.setLocations(List.of(getCredentialIssuerMetadata().getCredentialIssuer()));

            // Set the redirect URI from the client's configuration
            if (client.getRedirectUris() != null && !client.getRedirectUris().isEmpty()) {
                oauth.redirectUri(client.getRedirectUris().get(0));
            }

            String authCode = getAuthorizationCode(oauth, client, username, scope);
            return getBearerToken(oauth, authCode, authDetail);
        }

        // For non-credential scopes, use the appropriate flow based on client configuration
        if (client.isDirectAccessGrantsEnabled()) {
            return getBearerTokenDirectAccess(oauth, client, username, scope);
        } else {
            return getBearerTokenCodeFlow(oauth, client, username, scope);
        }
    }

    private List<OID4VCAuthorizationDetail> extractAuthorizationDetails(AccessTokenResponse tokenResponse) {
        // First check if already populated in token response
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        if (authDetailsResponse != null && !authDetailsResponse.isEmpty()) {
            return authDetailsResponse;
        }

        // Otherwise, extract from JWT access token
        try {
            JsonWebToken jwt = new JWSInput(tokenResponse.getAccessToken()).readJsonContent(JsonWebToken.class);
            Object authDetails = jwt.getOtherClaims().get(OAuth2Constants.AUTHORIZATION_DETAILS);
            if (authDetails != null) {
                return JsonSerialization.readValue(
                        JsonSerialization.writeValueAsString(authDetails),
                        new TypeReference<>() {}
                );
            }
        } catch (Exception e) {
            // Ignore - authorization_details not present or couldn't be parsed
        }
        return null;
    }

    private String getBearerToken(String clientId, String username, String scope) {
        return getBearerTokenResponse(clientId, username, scope).getAccessToken();
    }

    private String getBearerToken(String clientId, String username, String scope, OID4VCAuthorizationDetail... authDetail) {
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);
        String authCode = getAuthorizationCode(oauth, client, username, scope);
        return getBearerToken(oauth, authCode, authDetail).getAccessToken();
    }

    private String getBearerTokenAndLogout(String clientId, String userId, String scope) {
        String token = getBearerToken(clientId, userId, scope);
        logout(userId);
        return token;
    }

    private void logout(String userId) {
        findUserByUsernameId(testRealm(), userId).logout();
    }

    private CredentialOfferURI getCredentialOfferUri(TestContext ctx, String token) throws Exception {
        String credConfigId = ctx.credentialConfiguration.getId();
        CredentialOfferUriResponse credentialOfferURIResponse = oauth.oid4vc()
                .credentialOfferUriRequest(credConfigId)
                .preAuthorized(ctx.preAuthorized)
                .txCode(ctx.preAuthorized)
                .targetUser(ctx.appUser)
                .bearerToken(token)
                .send();
        CredentialOfferURI credentialOfferURI = credentialOfferURIResponse.getCredentialOfferURI();
        assertTrue(credentialOfferURI.getIssuer().startsWith(ctx.issuerMetadata.getCredentialIssuer()));
        assertTrue(Strings.isNotEmpty(credentialOfferURI.getNonce()));
        return credentialOfferURI;
    }

    private CredentialsOffer getCredentialsOffer(TestContext ctx, CredentialOfferURI credOfferUri) throws Exception {
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().doCredentialOfferRequest(credOfferUri);
        CredentialsOffer credOffer = credentialOfferResponse.getCredentialsOffer();
        assertEquals(List.of(ctx.credentialConfiguration.getId()), credOffer.getCredentialConfigurationIds());
        return credOffer;
    }

    private AccessTokenResponse getPreAuthorizedAccessTokenResponse(CredentialsOffer credOffer) throws Exception {
        PreAuthorizedCode preAuthCodeGrant = credOffer.getGrants().getPreAuthorizedCode();
        String preAuthCode = preAuthCodeGrant.getPreAuthorizedCode();
        String txCode = getTestingClient().testing().getTxCode(preAuthCode);
        return oauth.oid4vc().preAuthorizedCodeGrantRequest(preAuthCode)
                .txCode(txCode)
                .send();
    }

    private CredentialResponse getCredentialByAuthDetail(String accessToken, OID4VCAuthorizationDetail authDetail) throws Exception {
        var credentialRequest = new CredentialRequest();
        if (authDetail.getCredentialIdentifiers() != null) {
            credentialRequest.setCredentialIdentifier(authDetail.getCredentialIdentifiers().get(0));
        } else if (authDetail.getCredentialConfigurationId() == null) {
            credentialRequest.setCredentialConfigurationId(authDetail.getCredentialConfigurationId());
        }
        return sendCredentialRequest(accessToken, credentialRequest);
    }

    private CredentialResponse getCredentialByOffer(String accessToken, AccessTokenResponse tokenResponse, CredentialsOffer credOffer) throws Exception {
        List<String> credConfigIds = credOffer.getCredentialConfigurationIds();
        if (credConfigIds.size() > 1)
            throw new IllegalStateException("Multiple credential configuration ids not supported in: " + JsonSerialization.valueAsString(credOffer));
        var credentialRequest = new CredentialRequest();

        // Extract authorization_details (from token response or JWT)
        List<OID4VCAuthorizationDetail> authDetailsResponse = extractAuthorizationDetails(tokenResponse);

        if (authDetailsResponse != null && !authDetailsResponse.isEmpty()) {
            // If authorization_details are present, credential_identifier is required
            if (authDetailsResponse.get(0).getCredentialIdentifiers() != null &&
                    !authDetailsResponse.get(0).getCredentialIdentifiers().isEmpty()) {
                String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
                credentialRequest.setCredentialIdentifier(credentialIdentifier);
            } else {
                throw new IllegalStateException("authorization_details present but no credential_identifier found");
            }
        } else {
            // No authorization_details, use credential_configuration_id
            credentialRequest.setCredentialConfigurationId(credConfigIds.get(0));
        }

        return sendCredentialRequest(accessToken, credentialRequest);
    }

    private CredentialResponse sendCredentialRequest(String accessToken, CredentialRequest credRequest) {

        Oid4vcCredentialResponse credRequestResponse = oauth.oid4vc()
                .credentialRequest(credRequest)
                .bearerToken(accessToken)
                .send();

        CredentialResponse credResponse = credRequestResponse.getCredentialResponse();
        assertNotNull("The credentials array should be present in the response", credResponse.getCredentials());
        assertFalse("The credentials array should not be empty", credResponse.getCredentials().isEmpty());
        return credResponse;
    }

    private void verifyCredentialResponse(TestContext ctx, CredentialResponse credResponse) throws Exception {

        String issuer = ctx.issuerMetadata.getCredentialIssuer();
        String scope = ctx.credentialConfiguration.getScope();
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull("The first credential in the array should not be null", credentialObj);

        String expUsername = ctx.appUser != null ? ctx.appUser : appUsername;

        JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
        assertEquals(issuer, jsonWebToken.getIssuer());
        Object vc = jsonWebToken.getOtherClaims().get("vc");
        VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
        assertEquals(List.of(scope), credential.getType());
        assertEquals(URI.create(issuer), credential.getIssuer());
        assertEquals(expUsername + "@email.cz", credential.getCredentialSubject().getClaims().get("email"));
    }

    private void verifyTokenJwt(
            String token,
            List<String> includeScopes,
            List<String> excludeScopes,
            List<String> includeRoles,
            List<String> excludeRoles
    ) throws Exception {
        JsonWebToken jwt = JsonSerialization.readValue(new JWSInput(token).getContent(), JsonWebToken.class);
        List<String> wasScopes = Arrays.stream(((String) jwt.getOtherClaims().get("scope")).split("\\s")).toList();
        includeScopes.forEach(it -> assertTrue("Missing scope: " + it, wasScopes.contains(it)));
        excludeScopes.forEach(it -> assertFalse("Invalid scope: " + it, wasScopes.contains(it)));

        List<String> allRoles = new ArrayList<>();
        Object realmAccess = jwt.getOtherClaims().get("realm_access");
        if (realmAccess != null) {
            var realmRoles = ((Map<String, List<String>>) realmAccess).get("roles");
            allRoles.addAll(realmRoles);
        }
        Object resourceAccess = jwt.getOtherClaims().get("resource_access");
        if (resourceAccess != null) {
            var resourceAccessMapping = (Map<String, Map<String, List<String>>>) resourceAccess;
            resourceAccessMapping.forEach((k, v) -> {
                allRoles.addAll(v.get("roles"));
            });
        }
        includeRoles.forEach(it -> assertTrue("Missing role: " + it, allRoles.contains(it)));
        excludeRoles.forEach(it -> assertFalse("Invalid role: " + it, allRoles.contains(it)));
    }
}
