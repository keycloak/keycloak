package org.keycloak.tests.oid4vc;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.IDTokenRequest;
import org.keycloak.protocol.oid4vc.model.IDTokenRequestBuilder;
import org.keycloak.protocol.oid4vc.model.IDTokenResponse;
import org.keycloak.protocol.oid4vc.model.IDTokenResponseBuilder;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationRedirectResponse;
import org.keycloak.testsuite.util.oauth.IDTokenResponseRequest;
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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.VCTestRealmConfig.TEST_REALM_NAME;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIALS_OFFER_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIAL_OFFER_URI_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.CREDENTIAL_RESPONSE_ATTACHMENT_KEY;
import static org.keycloak.tests.oid4vc.OID4VCTestContext.ISSUER_METADATA_ATTACHMENT_KEY;
import static org.keycloak.util.DIDUtils.encodeDidKey;

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

    public CredentialsOffer createAuthCodeCredentialOffer(OID4VCTestContext ctx, String targetUser) {

        // Get Issuer AccessToken
        //
        AccessTokenResponse issTokenResponse = getIssuerAccessToken(ctx.issuer);

        // Exclude scope: <credScope>
        // Require role: credential-offer-create
        String issToken = validateIssuerAccessToken(issTokenResponse,
                List.of(), List.of(ctx.credScopeName),
                List.of(CREDENTIAL_OFFER_CREATE.getName()), List.of());

        // Create Authorized Code CredentialOffer
        //
        CredentialOfferURI credOfferUri;
        try {
            credOfferUri = createCredentialOffer(ctx, ctx.credConfigId)
                    .preAuthorized(false)
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

        String issuerState = credOffer.getIssuerState();
        assertNotNull(issuerState, "No IssuerState");

        return credOffer;
    }

    public CredentialsOffer createPreAuthCredentialOffer(OID4VCTestContext ctx, String targetUser, boolean withTxCode) {

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
                    .txCode(withTxCode)
                    .targetUser(targetUser)
                    .bearerToken(issToken)
                    .send().getCredentialOfferURI();
        } finally {
            logout(ctx.issuer);
        }

        if (withTxCode) {
            String txCode = credOfferUri.getTxCode();
            assertNotNull(txCode, "No TxCode");
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

    public PreAuthorizedCodeGrantRequest preAuthAccessTokenRequest(OID4VCTestContext ctx, String preAuthCode, String txCode) {
        PreAuthorizedCodeGrantRequest request = new PreAuthorizedCodeGrantRequest(oauth, preAuthCode) {
            public AccessTokenResponse send() {
                AccessTokenResponse response = super.send();
                ctx.putAttachment(ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY, response);
                return response;
            }
        }.txCode(txCode);
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

        assertEquals(ctx.credConfigId, tokenAuthDetail.getCredentialConfigurationId());
        assertEquals(tokenAuthDetail, jwtAuthDetail);

        return accessToken;
    }

    public static class AuthorizationEndpointRequest {

        private static final Logger log = Logger.getLogger(AuthorizationEndpointRequest.class);

        private final AbstractOAuthClient<?> client;
        private final LoginUrlBuilder loginForm;
        private CredentialIssuer issuerMetadata;
        private String clientId;
        private KeyPair keyPair;
        private String username;
        private String password;

        public AuthorizationEndpointRequest(AbstractOAuthClient<?> client) {
            this.client = client;
            this.loginForm = client.loginForm();
        }

        public AuthorizationEndpointRequest authorizationDetails(OID4VCAuthorizationDetail authDetail) {
            loginForm.authorizationDetails(List.of(authDetail));
            return this;
        }

        public AuthorizationEndpointRequest client(String clientId) {
            loginForm.clientId(clientId);
            this.clientId = clientId;
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
            loginForm.scope(scopes);
            return this;
        }

        public AuthorizationEndpointRequest issuerMetadata(CredentialIssuer issuerMetadata) {
            this.issuerMetadata = issuerMetadata;
            return this;
        }

        public AuthorizationEndpointRequest subjectKeyPair(KeyPair keyPair) {
            this.keyPair = keyPair;
            return this;
        }

        public AuthorizationEndpointResponse send(String username, String password) {
            this.username = username;
            this.password = password;
            return send();
        }

        public AuthorizationEndpointResponse send() {

            // Handle IDToken Authorization
            if (clientId != null && clientId.startsWith("did:")) {
                String endpointUrl = loginForm.build();
                return handleIDTokenAuthorization(endpointUrl);
            }

            loginForm.open();
            WebDriver driver = client.getDriver();
            AuthPageState authPageState = waitForLoginOrError(driver);
            switch (authPageState) {
                case ERROR_REDIRECT -> {
                    return new AuthorizationEndpointResponse(client);
                }
                case ERROR_PAGE -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("error", "invalid_request");
                    driver.findElements(By.id("kc-error-message")).stream()
                            .map(d -> d.getText().trim().split("\\n")[0])
                            .forEach(s -> params.put("error_description", s));
                    return new AuthorizationEndpointResponse(params);
                }
                // AuthPageState.LOGIN
                default -> {
                    client.fillLoginForm(username, password);
                    return new AuthorizationEndpointResponse(client);
                }
            }
        }

        enum AuthPageState {LOGIN, ERROR_REDIRECT, ERROR_PAGE}

        private AuthPageState waitForLoginOrError(WebDriver driver) {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            return wait.until(d -> {
                String url = d.getCurrentUrl();

                // Error via redirect (some flows)
                if (url != null && (url.contains("error=") || url.contains("error_description="))) {
                    return AuthPageState.ERROR_REDIRECT;
                }

                // Error page rendered by Keycloak
                if (!d.findElements(By.id("kc-error-message")).isEmpty()) {
                    return AuthPageState.ERROR_PAGE;
                }

                // Login form present
                if (!d.findElements(By.id("username")).isEmpty()
                        || !d.findElements(By.name("username")).isEmpty()) {
                    return AuthPageState.LOGIN;
                }

                return null; // keep waiting
            });
        }

        private AuthorizationEndpointResponse handleIDTokenAuthorization(String endpointUrl) {

            // Disable redirects for the HttpClients
            client.httpClient().set(HttpClients.custom().disableRedirectHandling().build());

            AuthorizationEndpointResponse response;
            try {
                HttpGet get = new HttpGet(endpointUrl);

                String location = new AuthorizationRedirectResponse(client.httpClient().get().execute(get))
                        .getRedirectLocation();

                // Wallet receives the IDTokenRequest
                //
                IDTokenRequest idTokenRequest = IDTokenRequestBuilder.fromUri(location).build();
                log.infof("Received IDTokenRequest: %s", JsonSerialization.valueAsString(idTokenRequest));

            /* Wallet gets the Issuer's PublicKey to verify the IDTokenRequest signature
            String kid = idTokenRequest.getJWSInput().getHeader().getKeyId();
            KeyMetadataRepresentation key = testRealm().keys().getKeyMetadata().getKeys().stream()
                    .filter(kmd -> kmd.getKid().equals(kid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No key for: " + kid));
            PublicKey publicKey = DerUtils.decodePublicKey(key.getPublicKey(), key.getType());

            // Wallet verifies the IDTokenRequest
            //
            idTokenRequest.verify(publicKey);
            */

                // Wallet creates/sends the IDTokenResponse
                //
                IDTokenResponse idTokenResponse = createIDTokenResponse(idTokenRequest);

                log.infof("Send IDTokenResponse: %s", JsonSerialization.valueAsString(idTokenResponse));
                response = new IDTokenResponseRequest(client, idTokenRequest.getRedirectUri(), idTokenResponse).send();

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                client.httpClient().reset();
            }
            return response;
        }

        private IDTokenResponse createIDTokenResponse(IDTokenRequest idTokenRequest) {
            String aud = issuerMetadata.getCredentialIssuer();
            String clientId = idTokenRequest.getClientId();

            ECPublicKey expPubKey = (ECPublicKey) keyPair.getPublic();
            String didKey = encodeDidKey(expPubKey);
            if (clientId.equals(didKey))
                throw new IllegalStateException("Unexpected IDToken client_id: " + clientId);

            IDTokenResponse idTokenResponse = new IDTokenResponseBuilder()
                    .withJwtIssuer(didKey)
                    .withJwtSubject(didKey)
                    .withJwtAudience(aud)
                    .sign(keyPair)
                    .build();

            return idTokenResponse;
        }
    }
}
