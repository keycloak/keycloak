package org.keycloak.tests.oid4vc;

import java.io.IOException;
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
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oid4vc.model.Proofs;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
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
import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.TEST_PASSWORD;
import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestRealmConfig.TEST_REALM_NAME;
import static org.keycloak.tests.oid4vc.OID4VCProofTestUtils.createEcKeyPair;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.AUTHORIZATION_SERVER_METADATA_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.AttachmentKey;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIALS_OFFER_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIALS_OFFER_URI_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIAL_RESPONSE_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.ISSUER_METADATA_ATTACHMENT_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A specialized Wallet facade for OID4VCI integration tests.
 *
 * <p>This class orchestrates OID4VCI message flows (Credential Offer, Authorization Request,
 * Token Exchange, Credential Request) using the Keycloak test framework.
 *
 * <p>It maintains internal session state (tracks logged-in users) to facilitate
 * automated cleanup and mimics the behavior of a mobile wallet app.
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class OID4VCBasicWallet {

    private final Keycloak keycloak;
    private final OAuthClient oauth;

    final Set<String> loginUsers = new HashSet<>();

    public OID4VCBasicWallet(Keycloak keycloak, OAuthClient oauth) {
        this.keycloak = keycloak;
        this.oauth = oauth;
    }

    /**
     * A composite action to create a credential offer with 'authorization_code' grant
     */
    public CredentialsOffer createCredentialOfferAuthCode(OID4VCTestContext ctx, String targetUser) {

        // Get Issuer AccessToken
        //
        AccessTokenResponse issTokenResponse = getAccessToken(ctx.getIssuer());

        // Exclude scope: <credScope>
        // Require role: credential-offer-create
        String issToken = validateIssuerAccessToken(issTokenResponse,
                List.of(), List.of(ctx.getScope()),
                List.of(CREDENTIAL_OFFER_CREATE.getName()), List.of());

        // Create Authorized Code CredentialOffer
        //
        CredentialOfferURI credOfferUri;
        try {
            credOfferUri = createCredentialOffer(ctx, ctx.getCredentialConfigurationId())
                    .preAuthorized(false)
                    .targetUser(targetUser)
                    .bearerToken(issToken)
                    .send().getCredentialOfferURI();
        } finally {
            logout(ctx.getIssuer());
        }

        // Fetch the CredentialsOffer
        //
        CredentialsOffer credOffer = credentialsOfferRequest(ctx, credOfferUri)
                .send().getCredentialsOffer();

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        return credOffer;
    }

    /**
     * A composite actions to create a credential offer with 'pre-authorized_code' grant
     */
    public CredentialsOffer createCredentialOfferPreAuth(OID4VCTestContext ctx, String targetUser) {

        // Get Issuer AccessToken
        //
        AccessTokenResponse issTokenResponse = getAccessToken(ctx.getIssuer());
        assertNotNull(issTokenResponse.getAccessToken(), "No accessToken");

        // Exclude scope: <credScope>
        // Require role: credential-offer-create
        String issToken = validateIssuerAccessToken(issTokenResponse,
                List.of(), List.of(ctx.getScope()),
                List.of(CREDENTIAL_OFFER_CREATE.getName()), List.of());

        // Create Pre-Authorized CredentialOffer
        //
        CredentialOfferURI credOfferUri;
        try {
            credOfferUri = createCredentialOffer(ctx, ctx.getCredentialConfigurationId())
                    .preAuthorized(true)
                    .targetUser(targetUser)
                    .bearerToken(issToken)
                    .send().getCredentialOfferURI();
        } finally {
            logout(ctx.getIssuer());
        }

        // Fetch the CredentialsOffer
        //
        CredentialsOffer credOffer = credentialsOfferRequest(ctx, credOfferUri)
                .send().getCredentialsOffer();

        String preAuthCode = credOffer.getPreAuthorizedCode();
        assertNotNull(preAuthCode, "No PreAuth Code");

        return credOffer;
    }

    public CredentialOfferUriRequest createCredentialOffer(OID4VCTestContext ctx, String credConfigId) {
        CredentialOfferUriRequest request = new CredentialOfferUriRequest(oauth, credConfigId) {
            public CredentialOfferUriResponse send() {
                CredentialOfferUriResponse response = super.send();
                ctx.putAttachment(CREDENTIALS_OFFER_URI_ATTACHMENT_KEY, response.getCredentialOfferURI());
                return response;
            }
        };
        return request;
    }

    public AccessTokenResponse getAccessToken(String username, String... scope) {
        PkceGenerator pkce = PkceGenerator.s256();
        AuthorizationEndpointResponse authResponse = authorizationRequest()
                .scope(scope)
                .codeChallenge(pkce)
                .send(username, TEST_PASSWORD);
        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No auth code");
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .codeVerifier(pkce)
                .send();
        assertNotNull(tokenResponse.getAccessToken(), "No AccessToken");
        return tokenResponse;
    }

    public CredentialIssuer getIssuerMetadata(OID4VCTestContext ctx) {
        var issuerMetadata = Optional.ofNullable(ctx.getAttachment(ISSUER_METADATA_ATTACHMENT_KEY))
                .orElse(oauth.oid4vc().doIssuerMetadataRequest().getMetadata());
        ctx.putAttachment(ISSUER_METADATA_ATTACHMENT_KEY, issuerMetadata);
        return issuerMetadata;
    }

    public OIDCConfigurationRepresentation getAuthorizationServerMetadata(OID4VCTestContext ctx) {
        var authServerMetadata = Optional.ofNullable(ctx.getAttachment(AUTHORIZATION_SERVER_METADATA_ATTACHMENT_KEY))
                .orElse(oauth.doWellKnownRequest());
        ctx.putAttachment(AUTHORIZATION_SERVER_METADATA_ATTACHMENT_KEY, authServerMetadata);
        return authServerMetadata;
    }

    public AuthorizationEndpointRequest authorizationRequest() {
        AuthorizationEndpointRequest request = new AuthorizationEndpointRequest() {
            public AuthorizationEndpointResponse send(String username, String password) {
                loginUsers.add(username);
                return super.send(username, password);
            }
        };
        return request;
    }

    public Proofs generateJwtProof(OID4VCTestContext ctx, String user) {
        String aud = getIssuerMetadata(ctx).getCredentialIssuer();
        String nonce = oauth.oid4vc().nonceRequest().send().getNonce();
        KeyWrapper kw = getECKeyPair(ctx, user, null);
        return Proofs.create(ProofType.JWT, OID4VCProofTestUtils.generateJwtProof(aud, kw, nonce));
    }

    public KeyWrapper getECKeyPair(OID4VCTestContext ctx, String user, String keyId) {
        String cacheKey = user + (keyId != null ? "_" + keyId : "");
        AttachmentKey<KeyWrapper> attachmentKey = new AttachmentKey<>(cacheKey, KeyWrapper.class);
        KeyWrapper kw = ctx.getAttachment(attachmentKey);
        if (kw == null) {
            kw = createEcKeyPair();
            ctx.putAttachment(attachmentKey, kw);
        }
        return kw;
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

    public PreAuthorizedCodeGrantRequest accessTokenRequestPreAuth(OID4VCTestContext ctx, String preAuthCode) {
        PreAuthorizedCodeGrantRequest request = new PreAuthorizedCodeGrantRequest(oauth, preAuthCode) {
            public AccessTokenResponse send() {
                AccessTokenResponse response = super.send();
                ctx.putAttachment(ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY, response);
                return response;
            }
        };
        return request;
    }

    public CredentialOfferRequest credentialsOfferRequest(OID4VCTestContext ctx, CredentialOfferURI credOfferUri) {
        CredentialOfferRequest request = new CredentialOfferRequest(oauth, credOfferUri) {
            public CredentialOfferResponse send() {
                CredentialOfferResponse response = super.send();
                ctx.putAttachment(CREDENTIALS_OFFER_ATTACHMENT_KEY, response.getCredentialsOffer());
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
        };
        request.bearerToken(accessToken);
        return request;
    }

    /**
     * Terminates all active user sessions tracked by this wallet instance.
     * Recommended to be called in {@code @AfterEach} methods.
     */
    public void logout() {
        for (String user : loginUsers) {
            logout(user);
        }
        loginUsers.clear();
    }

    /**
     * Terminates a specific user session via the Keycloak Admin API.
     *
     * @param username the username of the user to logout.
     */
    public void logout(String username) {
        RealmResource realm = keycloak.realm(TEST_REALM_NAME);
        UserRepresentation userRep = realm.users().search(username, true).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found for logout: " + username));
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
    ) {

        String accessToken = tokenResponse.getAccessToken();

        JsonWebToken jwt;
        try {
            jwt = JsonSerialization.readValue(new JWSInput(accessToken).getContent(), JsonWebToken.class);
        } catch (IOException | JWSInputException ex) {
            throw new IllegalStateException(ex);
        }

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

    public String validateHolderAccessToken(OID4VCTestContext ctx, AccessTokenResponse tokenResponse) {

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

        JsonWebToken jwt;
        try {
            jwt = new JWSInput(tokenResponse.getAccessToken()).readJsonContent(JsonWebToken.class);
        } catch (JWSInputException ex) {
            throw new IllegalStateException(ex);
        }

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

        assertEquals(ctx.getCredentialConfigurationId(), tokenAuthDetail.getCredentialConfigurationId());
        assertEquals(tokenAuthDetail, jwtAuthDetail);

        return accessToken;
    }

    /**
     * The Authorization request/response follows the known pattern that we have for other message exchanges on
     * 'OID4VCClient'. The response does not only capture a successful authorization, but (like the other APIs)
     * also error cases, like ...
     *
     *      - login page does not get displayed, because of an invalid request
     *      - invalid credentials on login page (i.e. redirect url without code)
     *
     * In all three cases (i.e. success + 2x error cases), we like to return a response as soon as we have it,
     * instead of waiting for a timeout like we currently have with authorization through the 'OAuthClient'.
     *
     * This API also abstracts to the low level details of web driver and login forms that are specific for the interactive
     * Authorization Code Flow. In future, we can use the same API for non-interactive flows (e.g. as required by EBSI)
     *
     * Historically, this Authorization request abstractions was with the 'OID4VCClient', rather than with the 'OAuthClient'
     * because we did not want to expose all Keycloak tests to go through this still experimental code. It never got
     * approved there and is now only available through the Wallet. We can assume that code similar to this would be found
     * in a real OID4VCI Wallet.
     *
     * 'OID4VCPublicClientTest' covers authorization success and the error cases.
     */
    public class AuthorizationEndpointRequest {

        protected final LoginUrlBuilder loginForm;

        public AuthorizationEndpointRequest() {
            this.loginForm = oauth.loginForm();
        }

        public AuthorizationEndpointRequest authorizationDetails(OID4VCAuthorizationDetail authDetail) {
            loginForm.authorizationDetails(List.of(authDetail));
            return this;
        }

        public AuthorizationEndpointRequest codeChallenge(PkceGenerator pkce) {
            loginForm.codeChallenge(pkce);
            return this;
        }

        public AuthorizationEndpointRequest issuerState(String issuerState) {
            loginForm.issuerState(issuerState);
            return this;
        }

        public AuthorizationEndpointRequest request(String request) {
            loginForm.request(request);
            return this;
        }

        public AuthorizationEndpointRequest scope(String... scopes) {
            if (scopes != null && scopes.length > 0) {
                loginForm.scope(scopes);
            }
            return this;
        }

        public void openLoginForm() {
            loginForm.open();
        }

        public AuthorizationEndpointResponse send(String username, String password) {
            // [TODO #47649] OAuthClient cannot handle invalid authorization requests
            // https://github.com/keycloak/keycloak/issues/47649
            openLoginForm();
            oauth.fillLoginForm(username, password);
            return oauth.parseLoginResponse();
        }
    }
}
