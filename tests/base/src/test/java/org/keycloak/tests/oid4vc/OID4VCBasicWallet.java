package org.keycloak.tests.oid4vc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LoginUrlBuilder;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferRequest;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriRequest;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialRequest;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.PreAuthorizedCodeGrantRequest;
import org.keycloak.util.JsonSerialization;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestRealmConfig.TEST_REALM_NAME;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIALS_OFFER_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIAL_OFFER_URI_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIAL_RESPONSE_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.ISSUER_METADATA_ATTACHMENT_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A basic Wallet to exercise various OID4VCI message flows.
 *
 * Wallet state between messages is maintained in {@code OID4VCTestContext}.
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class OID4VCBasicWallet {

    final Keycloak keycloak;
    final OAuthClient oauth;

    final Set<String> loginUsers = new HashSet<>();

    public OID4VCBasicWallet(Keycloak keycloak, OAuthClient oauth) {
        this.keycloak = keycloak;
        this.oauth = oauth;
    }

    // Composite Actions -----------------------------------------------------------------------------------------------

    public CredentialsOffer createPreAuthCredentialOffer(OID4VCTestContext ctx, String targetUser) throws Exception {

        // Get Issuer AccessToken
        //
        AccessTokenResponse issTokenResponse = getIssuerAccessToken(ctx.issuer);
        assertNotNull(issTokenResponse.getAccessToken(), "No accessToken");

        // Exclude scope: <credScope>
        // Require role: credential-offer-create
        String issToken = validateIssuerAccessToken(issTokenResponse,
                List.of(), List.of(ctx.credScopeName),
                List.of(CREDENTIAL_OFFER_CREATE.getName()), List.of());

        // Create Pre-Authorized CredentialOffer
        //
        CredentialOfferURI credOfferUri;
        try {
            credOfferUri = createCredentialOffer(ctx, ctx.credConfigId)
                    .preAuthorized(true)
                    .targetUser(targetUser)
                    .bearerToken(issToken)
                    .send().getCredentialOfferURI();
        } finally {
            logout(ctx.issuer);
        }

        // Fetch the CredentialsOffer
        //
        CredentialsOffer credOffer = getCredentialOffer(ctx, credOfferUri)
                .send().getCredentialsOffer();

        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "No PreAuth Code");

        return credOffer;
    }

    // Low Level Messages ----------------------------------------------------------------------------------------------

    public CredentialOfferUriRequest createCredentialOffer(OID4VCTestContext ctx, String credConfigId) {
        CredentialOfferUriRequest request = new CredentialOfferUriRequest(oauth, credConfigId) {
            public CredentialOfferUriResponse send() {
                CredentialOfferUriResponse response = super.send();
                ctx.putAttachment(CREDENTIAL_OFFER_URI_ATTACHMENT_KEY, response.getCredentialOfferURI());
                return response;
            }
        };
        return request;
    }

    public CredentialOfferRequest getCredentialOffer(OID4VCTestContext ctx, CredentialOfferURI credOfferUri) {
        CredentialOfferRequest request = new CredentialOfferRequest(oauth, credOfferUri) {
            public CredentialOfferResponse send() {
                CredentialOfferResponse response = super.send();
                ctx.putAttachment(CREDENTIALS_OFFER_ATTACHMENT_KEY, response.getCredentialsOffer());
                return response;
            }
        };
        return request;
    }

    public AuthorizationEndpointRequest authorizationRequest() {
        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest(oauth) {
            public AuthorizationEndpointResponse send(String username, String password) {
                loginUsers.add(username);
                return super.send(username, password);
            }
        };
        return request;
    }

    public AccessTokenRequest accessTokenRequest(OID4VCTestContext ctx, String authCode) {
        AccessTokenRequest request = new AccessTokenRequest(oauth, authCode) {
            public AccessTokenResponse send() {
                AccessTokenResponse response = super.send();
                ctx.putAttachment(ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY, response);
                return response;
            }
        };
        return request;
    }

    public PreAuthorizedCodeGrantRequest preAuthAccessTokenRequest(OID4VCTestContext ctx, String preAuthCode) {
        PreAuthorizedCodeGrantRequest request = new PreAuthorizedCodeGrantRequest(oauth, preAuthCode) {
            public AccessTokenResponse send() {
                AccessTokenResponse response = super.send();
                ctx.putAttachment(ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY, response);
                return response;
            }
        };
        return request;
    }

    public Oid4vcCredentialRequest credentialRequest(OID4VCTestContext ctx, String accessToken) {
        Oid4vcCredentialRequest request = new Oid4vcCredentialRequest(oauth, new CredentialRequest()) {
            public Oid4vcCredentialResponse send() {
                Oid4vcCredentialResponse response = super.send();
                CredentialResponse credentialResponse = response.getCredentialResponse();
                ctx.putAttachment(CREDENTIAL_RESPONSE_ATTACHMENT_KEY, credentialResponse);
                return response;
            }
        }.bearerToken(accessToken);
        return request;
    }

    public AccessTokenResponse getIssuerAccessToken(String username) {
        PkceGenerator pkce = PkceGenerator.s256();
        AuthorizationEndpointResponse authResponse = authorizationRequest()
                .codeChallenge(pkce)
                .send(username, "password");

        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No authCode");
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .codeVerifier(pkce)
                .send();
        assertNotNull(tokenResponse.getAccessToken(), "No AccessToken");
        return tokenResponse;
    }

    public CredentialIssuer getIssuerMetadata(OID4VCTestContext ctx) {
        CredentialIssuer issuerMetadata = Optional.ofNullable(ctx.getAttachment(ISSUER_METADATA_ATTACHMENT_KEY))
                .orElse(oauth.oid4vc().doIssuerMetadataRequest().getMetadata());
        ctx.putAttachment(ISSUER_METADATA_ATTACHMENT_KEY, issuerMetadata);
        return issuerMetadata;
    }

    public void logout() {
        for (String user : loginUsers) {
            logout(user);
        }
        loginUsers.clear();
    }

    public void logout(String username) {
        RealmResource realm = keycloak.realm(TEST_REALM_NAME);
        UserRepresentation userRep = realm.users().search(username).get(0);
        UserResource userResource = realm.users().get(userRep.getId());
        userResource.logout();
    }

    // State Validation ------------------------------------------------------------------------------------------------

    public void verifyCredentialsSignature(CredentialResponse credResponse, String algorithm) throws Exception {
        for (Credential credEntry : credResponse.getCredentials()) {

            String encodedCredential = credEntry.getCredential().toString();
            JWSInput jwsInput = new JWSInput(encodedCredential);
            JWSHeader header = jwsInput.getHeader();

            assertEquals(algorithm, header.getRawAlgorithm());
            oauth.verifyToken(encodedCredential, JsonWebToken.class);
        }
    }

    public String validateIssuerAccessToken(
            AccessTokenResponse tokenResponse,
            List<String> includeScopes, List<String> excludeScopes,
            List<String> includeRoles, List<String> excludeRoles
    ) throws Exception {

        String accessToken = tokenResponse.getAccessToken();
        JsonWebToken jwt = JsonSerialization.readValue(new JWSInput(accessToken).getContent(), JsonWebToken.class);
        List<String> wasScopes = Arrays.stream(((String) jwt.getOtherClaims().get("scope")).split("\\s")).toList();
        includeScopes.forEach(it -> assertTrue(wasScopes.contains(it), "Missing scope: " + it));
        excludeScopes.forEach(it -> assertFalse(wasScopes.contains(it), "Invalid scope: " + it));

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
            resourceAccessMapping.forEach((k, v) ->
                allRoles.addAll(v.get("roles")));
        }
        includeRoles.forEach(it -> assertTrue(allRoles.contains(it), "Missing role: " + it));
        excludeRoles.forEach(it -> assertFalse(allRoles.contains(it), "Invalid role: " + it));

        return accessToken;
    }

    public String validateHolderAccessToken(OID4VCTestContext ctx, AccessTokenResponse tokenResponse) throws Exception {

        // Check that we can extract the AccessToken
        if (!tokenResponse.isSuccess()) {
            fail("Error in AccessToken response: " + tokenResponse.getErrorDescription());
        }

        String accessToken = tokenResponse.getAccessToken();
        assertNotNull(accessToken, "No AccessToken");

        // Extract authorization_details from AccessTokenResponse
        //
        List<OID4VCAuthorizationDetail> tokenAuthDetails = tokenResponse.getOID4VCAuthorizationDetails();
        assertTrue(tokenAuthDetails != null && !tokenAuthDetails.isEmpty(), "No authorization_details in AccessTokenResponse");

        // Extract authorization_details from AccessToken (JWT)
        //
        JsonWebToken jwt = new JWSInput(tokenResponse.getAccessToken()).readJsonContent(JsonWebToken.class);
        Object authDetailsClaim = jwt.getOtherClaims().get(AUTHORIZATION_DETAILS);
        String authDetailsJson = Optional.ofNullable(authDetailsClaim)
                .map(JsonSerialization::valueAsString)
                .orElse(null);
        List<OID4VCAuthorizationDetail> jwtAuthDetails = Optional.ofNullable(authDetailsJson)
                .map(it -> JsonSerialization.valueFromString(it, OID4VCAuthorizationDetail[].class))
                .map(Arrays::asList)
                .orElse(null);
        assertTrue(jwtAuthDetails != null && !jwtAuthDetails.isEmpty(), "No authorization_details in AccessTokenJWT");

        assertEquals(1, tokenAuthDetails.size(), "Expected one authorization_details entry");
        var tokenAuthDetail = tokenAuthDetails.get(0);

        assertEquals(1, jwtAuthDetails.size(), "Expected one authorization_details entry");
        var jwtAuthDetail = jwtAuthDetails.get(0);

        assertEquals(ctx.credConfigId, tokenAuthDetail.getCredentialConfigurationId());
        assertEquals(tokenAuthDetail, jwtAuthDetail);

        return accessToken;
    }

    public static class AuthorizationEndpointRequest {

        protected final AbstractOAuthClient<?> client;
        protected final LoginUrlBuilder loginForm;

        public AuthorizationEndpointRequest(AbstractOAuthClient<?> client) {
            this.client = client;
            this.loginForm = client.loginForm();
        }

        public AuthorizationEndpointRequest authorizationDetails(OID4VCAuthorizationDetail authDetail) {
            loginForm.authorizationDetails(List.of(authDetail));
            return this;
        }

        public AuthorizationEndpointRequest codeChallenge(PkceGenerator pkce) {
            loginForm.codeChallenge(pkce);
            return this;
        }

        public AuthorizationEndpointRequest request(String request) {
            loginForm.request(request);
            return this;
        }

        public AuthorizationEndpointRequest scope(String... scopes) {
            loginForm.scope(scopes);
            return this;
        }

        public AuthorizationEndpointResponse send(String username, String password) {
            loginForm.open();
            client.fillLoginForm(username, password);
            return client.parseLoginResponse();
        }
    }
}
