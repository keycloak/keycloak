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

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.AuthorizationRequestBuilder;
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
import org.keycloak.protocol.oidc.utils.PkceGenerator;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.oid4vc.issuance.signing.OID4VCIssuerEndpointTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationRequestRequest;
import org.keycloak.testsuite.util.oauth.AuthorizationRequestResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.directory.api.util.Strings;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;
import static org.keycloak.OAuth2Constants.SCOPE_OPENID;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.protocol.oid4vc.model.ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.clientId;
import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.namedClientId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Credential Offer Validity Matrix
 * <p>
 * +----------+-----------+----------+---------+------------------------------------------------------+
 * | pre-auth | clientId  | username | Valid   | Notes                                                |
 * +----------+-----------+----------+---------+------------------------------------------------------+
 * | no       | no        | no       | yes     | Generic offer; any logged-in user may redeem.        |
 * | no       | no        | yes      | yes     | Offer restricted to a specific user.                 |
 * | no       | yes       | no       | yes     | Bound to client; user determined at login.           |
 * | no       | yes       | yes      | yes     | Bound to both client and user.                       |
 * +----------+-----------+----------+---------+------------------------------------------------------+
 * | yes      | no        | no       | no      | Pre-auth requires a user subject; missing username.  |
 * | yes      | no        | yes      | yes     | Pre-auth for a specific user; client issuer defined. |
 * | yes      | yes       | no       | no      | Same as above; username required.                    |
 * | yes      | yes       | yes      | yes     | Fully constrained: user + client.                    |
 * +----------+-----------+----------+---------+------------------------------------------------------+
 */
public class OID4VCICredentialOfferMatrixTest extends OID4VCIssuerEndpointTest {

    String issUsername = "john";
    String issClientId = clientId;

    String appUsername = "alice";

    String credScopeName = jwtTypeNaturalPersonScopeName;
    String credConfigId = jwtTypeNaturalPersonScopeName;

    static class OfferTestContext {
        boolean preAuthorized;
        String issUser;
        String issClient;
        String targetUser;
        String targetClient;
        CredentialIssuer issuerMetadata;
        OIDCConfigurationRepresentation authorizationMetadata;
        SupportedCredentialConfiguration supportedCredentialConfiguration;
    }

    OfferTestContext newTestContext(boolean preAuth, String targetClient, String targetUser) {
        var ctx = new OfferTestContext();
        ctx.preAuthorized = preAuth;
        ctx.issUser = issUsername;
        ctx.issClient = issClientId;
        ctx.targetUser = targetUser;
        ctx.targetClient = targetClient;
        ctx.issuerMetadata = getCredentialIssuerMetadata();
        ctx.authorizationMetadata = getAuthorizationMetadata(ctx.issuerMetadata.getAuthorizationServers().get(0));
        ctx.supportedCredentialConfiguration = ctx.issuerMetadata.getCredentialsSupported().get(credConfigId);
        return ctx;
    }

    @Test
    public void testCredentialWithoutOffer() throws Exception {

        var ctx = newTestContext(false, null, appUsername);

        String redirectUri = verifiedRedirectUri(clientId, oauth.getRedirectUri());
        PkceGenerator pkce = PkceGenerator.s256();

        // Build an AuthorizationRequest with AuthorizationDetails
        //
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(ctx.issuerMetadata.getCredentialIssuer()));

        AuthorizationRequest authRequest = new AuthorizationRequestBuilder()
                .withClientId(issClientId)
                // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
                .withScope(SCOPE_OPENID, credScopeName)
                .withAuthorizationDetail(authDetail)
                .withRedirectUri(redirectUri)
                .withCodeChallenge(pkce)
                .build();

        AuthorizationRequestResponse authResponse = new AuthorizationRequestRequest(oauth, authRequest)
                .credentials(ctx.targetUser, "password")
                .send();

        String authCode = authResponse.getCode();
        String accessToken = oauth.accessTokenRequest(authCode)
                .authorizationDetails(authDetail)
                .codeVerifier(pkce)
                .send()
                .getAccessToken();

        CredentialResponse credResponse = getCredentialByAuthDetail(ctx, accessToken, authDetail);
        verifyCredentialResponse(ctx, credResponse);
    }

    @Test
    public void testCredentialOffer_noPreAuth_noClientId_noUserId() throws Exception {
        runCredentialOfferTest(newTestContext(false, null, null));
    }

    @Test
    public void testCredentialOffer_noPreAuth_noClientId_UserId() throws Exception {
        runCredentialOfferTest(newTestContext(false, null, appUsername));
    }

    @Test
    public void testCredentialOffer_noPreAuth_ClientId_noUserId() throws Exception {
        runCredentialOfferTest(newTestContext(false, namedClientId, null));
    }

    @Test
    public void testCredentialOffer_noPreAuth_ClientId_UserId() throws Exception {
        runCredentialOfferTest(newTestContext(false, namedClientId, appUsername));
    }

    // Pre Authorized --------------------------------------------------------------------------------------------------

    @Test
    public void testCredentialOffer_PreAuth_noClientId_noUserId() throws Exception {
        try {
            runCredentialOfferTest(newTestContext(true, null, null));
            fail("Expected " + INVALID_CREDENTIAL_OFFER_REQUEST.name());
        } catch (RuntimeException ex) {
            List.of(INVALID_CREDENTIAL_OFFER_REQUEST.name(), "Pre-Authorized credential offer requires a target user")
                    .forEach(it -> assertTrue(ex.getMessage() + " does not contain " + it, ex.getMessage().contains(it)));
        }
    }

    @Test
    public void testCredentialOffer_PreAuth_noClientId_UserId() throws Exception {
        runCredentialOfferTest(newTestContext(true, null, appUsername));
    }

    @Test
    public void testCredentialOffer_PreAuth_ClientId_noUserId() throws Exception {
        try {
            runCredentialOfferTest(newTestContext(true, namedClientId, null));
            fail("Expected " + INVALID_CREDENTIAL_OFFER_REQUEST.name());
        } catch (RuntimeException ex) {
            List.of(INVALID_CREDENTIAL_OFFER_REQUEST.name(), "Pre-Authorized credential offer requires a target user")
                    .forEach(it -> assertTrue(ex.getMessage() + " does not contain " + it, ex.getMessage().contains(it)));
        }
    }

    @Test
    public void testCredentialOffer_PreAuth_ClientId_UserId() throws Exception {
        runCredentialOfferTest(newTestContext(true, namedClientId, appUsername));
    }

    @Test
    public void testCredentialOffer_PreAuth_ClientId_UserId_disabled() throws Exception {
        // Disable user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), appUsername);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setEnabled(false);
        user.update(userRep);

        try {
            runCredentialOfferTest(newTestContext(true, namedClientId, appUsername));
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

    void runCredentialOfferTest(OfferTestContext ctx) throws Exception {

        // Issuer login
        //
        String issToken = getIssuerAccessToken(ctx.issClient, ctx.issUser, SCOPE_OPENID);

        // Exclude scope: <credScope>
        // Include role: credential-offer-create
        verifyTokenJwt(issToken,
                List.of(SCOPE_OPENID), List.of(ctx.supportedCredentialConfiguration.getScope()),
                List.of(CREDENTIAL_OFFER_CREATE.getName()), List.of());

        // Retrieving the credential-offer-uri
        //
        CredentialOfferURI offerUri = getCredentialOfferUri(ctx, issToken);

        // Issuer logout in order to remove unwanted session state
        //
        logout(ctx.issUser);

        // Using the uri to get the actual credential offer
        //
        CredentialsOffer credOffer = getCredentialsOffer(ctx, offerUri);

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
            AccessTokenResponse accessToken = getPreAuthorizedAccessTokenResponse(ctx, credOffer);
            List<OID4VCAuthorizationDetail> authDetails = accessToken.getOid4vcAuthorizationDetails();
            if (authDetails == null || authDetails.isEmpty()) {
                throw new IllegalStateException("No authorization_details in token response");
            }
            if (authDetails.size() > 1) {
                throw new IllegalStateException("Multiple authorization_details in token response");
            }

            // Get the credential and verify
            //
            OID4VCAuthorizationDetail authDetail = authDetails.get(0);
            CredentialResponse credResponse = getCredentialByAuthDetail(ctx, accessToken.getAccessToken(), authDetail);
            verifyCredentialResponse(ctx, credResponse);

        } else {

            String clientId = ctx.targetClient != null ? ctx.targetClient : issClientId;
            String targetUser = ctx.targetUser != null ? ctx.targetUser : appUsername;
            String credConfigId = credOffer.getCredentialConfigurationIds().get(0);
            String credConfigScope = ctx.issuerMetadata.getCredentialsSupported().get(credConfigId).getScope();

            // Reconfigure the OAuthClient
            if (!clientId.equals(issClientId)) {
                ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);
                oauth.redirectUri(client.getRedirectUris().get(0));
                oauth.client(clientId, client.getSecret());
            }

            // Build an AuthorizationRequest with AuthorizationDetails
            //
            OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
            authDetail.setType(OPENID_CREDENTIAL);
            authDetail.setCredentialConfigurationId(credConfigId);
            authDetail.setLocations(List.of(ctx.issuerMetadata.getCredentialIssuer()));

            String redirectUri = verifiedRedirectUri(clientId, oauth.getRedirectUri());
            PkceGenerator pkce = PkceGenerator.s256();

            AuthorizationRequest authRequest = new AuthorizationRequestBuilder()
                    .withClientId(clientId)
                    // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
                    .withScope(SCOPE_OPENID, credConfigScope)
                    .withAuthorizationDetail(authDetail)
                    .withRedirectUri(redirectUri)
                    .withCodeChallenge(pkce)
                    .build();

            AuthorizationRequestResponse authResponse = new AuthorizationRequestRequest(oauth, authRequest)
                    .credentials(targetUser, "password")
                    .send();

            String authCode = authResponse.assertCode();
            AccessTokenResponse accessToken = oauth.accessTokenRequest(authCode)
                    .codeVerifier(pkce)
                    .send();

            // Get the credential and verify
            //
            CredentialResponse credResponse = getCredentialByOffer(accessToken.getAccessToken(), credOffer);
            verifyCredentialResponse(ctx, credResponse);
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private String verifiedRedirectUri(String clientId, String redirectUri) {
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);
        String registeredUri = client.getRedirectUris().stream()
                .filter(uri -> uri.startsWith(redirectUri))
                .findFirst()
                .orElse(null);
        if (registeredUri == null) {
            throw new IllegalArgumentException("Invalid redirect_uri: " + redirectUri);
        }
        return redirectUri;
    }

    private String getIssuerAccessToken(String clientId, String issUser, String scope) {
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);
        String accessToken = getBearerToken(oauth, client, issUser, scope);
        return accessToken;
    }

    private void logout(String userId) {
        findUserByUsernameId(testRealm(), userId).logout();
    }

    private CredentialOfferURI getCredentialOfferUri(OfferTestContext ctx, String token) {
        String credConfigId = ctx.supportedCredentialConfiguration.getId();
        String credOfferUriUrl = getCredentialOfferUriUrl(credConfigId, ctx.preAuthorized, ctx.targetUser, ctx.targetClient);
        CredentialOfferUriResponse credentialOfferURIResponse = oauth.oid4vc()
                .credentialOfferUriRequest()
                .endpoint(credOfferUriUrl)
                .bearerToken(token)
                .send();
        CredentialOfferURI credentialOfferURI = credentialOfferURIResponse.getCredentialOfferURI();
        assertTrue(credentialOfferURI.getIssuer().startsWith(ctx.issuerMetadata.getCredentialIssuer()));
        assertTrue(Strings.isNotEmpty(credentialOfferURI.getNonce()));
        return credentialOfferURI;
    }

    private CredentialsOffer getCredentialsOffer(OfferTestContext ctx, CredentialOfferURI credOfferURI) {
        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().doCredentialOfferRequest(credOfferURI);
        CredentialsOffer credOffer = credentialOfferResponse.getCredentialsOffer();
        assertEquals(List.of(ctx.supportedCredentialConfiguration.getId()), credOffer.getCredentialConfigurationIds());
        return credOffer;
    }

    private AccessTokenResponse getPreAuthorizedAccessTokenResponse(OID4VCICredentialOfferMatrixTest.OfferTestContext ctx, CredentialsOffer credOffer) {
        PreAuthorizedCode preAuthorizedCode = credOffer.getGrants().getPreAuthorizedCode();
        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(preAuthorizedCode.getPreAuthorizedCode())
                .endpoint(ctx.authorizationMetadata.getTokenEndpoint())
                .send();
        return accessTokenResponse;
    }

    private CredentialResponse getCredentialByAuthDetail(OfferTestContext ctx, String accessToken, OID4VCAuthorizationDetail authDetail) {
        List<String> credIdentifiers = authDetail.getCredentialIdentifiers();
        var credentialRequest = new CredentialRequest();
        if (credIdentifiers != null) {
            if (credIdentifiers.size() > 1)
                throw new IllegalStateException("Multiple credential ids not supported");
            credentialRequest.setCredentialIdentifier(credIdentifiers.get(0));
        } else {
            if (authDetail.getCredentialConfigurationId() == null)
                throw new IllegalStateException("No credential_configuration_id in: " + JsonSerialization.valueAsString(authDetail));
            credentialRequest.setCredentialConfigurationId(authDetail.getCredentialConfigurationId());
        }
        return sendCredentialRequest(accessToken, credentialRequest);
    }

    private CredentialResponse getCredentialByOffer(String accessToken, CredentialsOffer credOffer) {
        List<String> credConfigIds = credOffer.getCredentialConfigurationIds();
        if (credConfigIds.size() > 1)
            throw new IllegalStateException("Multiple credential configuration ids not supported in: " + JsonSerialization.valueAsString(credOffer));
        var credentialRequest = new CredentialRequest().setCredentialConfigurationId(credConfigIds.get(0));
        return sendCredentialRequest(accessToken, credentialRequest);
    }

    private CredentialResponse sendCredentialRequest(String accessToken, CredentialRequest credRequest) {
        CredentialResponse credentialResponse = oauth.oid4vc().credentialRequest(credRequest)
                .bearerToken(accessToken)
                .send().getCredentialResponse();
        assertNotNull("The credentials array should be present in the response", credentialResponse.getCredentials());
        assertFalse("The credentials array should not be empty", credentialResponse.getCredentials().isEmpty());
        return credentialResponse;
    }

    private void verifyCredentialResponse(OfferTestContext ctx, CredentialResponse credResponse) throws Exception {

        String issuer = ctx.issuerMetadata.getCredentialIssuer();
        String scope = ctx.supportedCredentialConfiguration.getScope();
        CredentialResponse.Credential credentialObj = credResponse.getCredentials().get(0);
        assertNotNull("The first credential in the array should not be null", credentialObj);

        String expUsername = ctx.targetUser != null ? ctx.targetUser : appUsername;

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
            @SuppressWarnings("unchecked")
            var realmRoles = ((Map<String, List<String>>) realmAccess).get("roles");
            allRoles.addAll(realmRoles);
        }
        Object resourceAccess = jwt.getOtherClaims().get("resource_access");
        if (resourceAccess != null) {
            @SuppressWarnings("unchecked")
            var resourceAccessMapping = (Map<String, Map<String, List<String>>>) resourceAccess;
            resourceAccessMapping.forEach((k, v) -> allRoles.addAll(v.get("roles")));
        }
        includeRoles.forEach(it -> assertTrue("Missing role: " + it, allRoles.contains(it)));
        excludeRoles.forEach(it -> assertFalse("Invalid role: " + it, allRoles.contains(it)));
    }
}
