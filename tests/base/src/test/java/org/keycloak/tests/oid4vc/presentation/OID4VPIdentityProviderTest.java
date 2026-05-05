package org.keycloak.tests.oid4vc.presentation;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.presentation.DirectPostResponse;
import org.keycloak.protocol.oid4vc.presentation.OID4VPConstants;
import org.keycloak.protocol.oid4vc.presentation.OID4VPIdentityProviderConfig;
import org.keycloak.protocol.oid4vc.presentation.OID4VPIdentityProviderFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeyWrapperUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = OID4VPIdentityProviderTest.DefaultServerConfigWithOid4Vp.class)
public class OID4VPIdentityProviderTest {

    private static final String CLIENT_ID = "oid4vp-test-client";
    private static final String CLIENT_SECRET = "oid4vp-test-secret";
    private static final String IDP_ALIAS = "oid4vp-idp";
    private static final String WALLET_SCHEME = OID4VPConstants.DEFAULT_WALLET_SCHEME;
    private static final String SUBJECT_CLAIM_NAME = "person_id";

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient(config = OID4VPClientConfig.class)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @AfterEach
    void cleanupBrowser() {
        driver.cookies().deleteAll();
        driver.open("about:blank");
    }

    @Test
    public void testBrowserCanResumeBrokerLoginFromVerifiedDirectPost() throws Exception {
        OID4VPBasicWallet wallet = setupWallet();
        AuthorizationRequest authorizationRequest = fetchAuthorizationRequest(wallet);
        String credential = createVerifiedCredential(authorizationRequest);

        DirectPostResponse directPostResponse = wallet.submitDirectPost(authorizationRequest, vpTokenFor(authorizationRequest, credential));
        assertThat(directPostResponse.getRedirectUri(), startsWith(realm.getBaseUrl() + "/broker/" + IDP_ALIAS + "/endpoint/continue"));

        AuthorizationEndpointResponse authorizationResponse = wallet.continueInBrowser(directPostResponse);
        assertThat(driver.getCurrentUrl(), startsWith(oauth.getRedirectUri()));
        assertNotNull(authorizationResponse.getCode(), "No authorization code");

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authorizationResponse.getCode()).send();
        assertTrue(tokenResponse.isSuccess(), tokenResponse.getErrorDescription());
    }

    @Test
    public void testRequestObjectEndpointAndDirectPostRejectsUnverifiedToken() throws Exception {
        OID4VPBasicWallet wallet = setupWallet();
        AuthorizationRequest authorizationRequest = fetchAuthorizationRequest(wallet);
        assertEquals(OID4VPConstants.RESPONSE_TYPE_VP_TOKEN, authorizationRequest.getResponseType());
        assertEquals(OID4VPConstants.RESPONSE_MODE_DIRECT_POST, authorizationRequest.getResponseMode());
        assertEquals(OID4VPConstants.AUD_SELF_ISSUED_V2, authorizationRequest.getAudience());
        assertNotNull(authorizationRequest.getResponseUri(), "No response_uri");
        assertNotNull(authorizationRequest.getDcqlQuery(), "No DCQL query");
        assertEquals(1, authorizationRequest.getDcqlQuery().getCredentials().size());
        assertEquals(List.of(SUBJECT_CLAIM_NAME), authorizationRequest.getDcqlQuery().getCredentials().get(0).getClaims().get(0).getPath());

        // The wallet sends a spec-shaped DCQL response with an unverifiable presentation.
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                wallet.submitDirectPostStatus(authorizationRequest, vpTokenFor(authorizationRequest, "dummy-vp-token")));
    }

    @Test
    public void testDirectPostRejectsMalformedVpToken() throws Exception {
        OID4VPBasicWallet wallet = setupWallet();
        AuthorizationRequest authorizationRequest = fetchAuthorizationRequest(wallet);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
                wallet.submitDirectPostStatus(authorizationRequest, "dummy-vp-token"));
    }

    public static class OID4VPClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId(CLIENT_ID).secret(CLIENT_SECRET);
        }
    }

    public static class DefaultServerConfigWithOid4Vp implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VP);
        }
    }

    private void createIdentityProvider() {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(IDP_ALIAS);
        idp.setProviderId(OID4VPIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        idp.setConfig(new HashMap<>());
        idp.getConfig().put(OID4VPIdentityProviderConfig.WALLET_SCHEME, WALLET_SCHEME);
        idp.getConfig().put(OID4VPIdentityProviderConfig.SUBJECT_CLAIM_NAME, SUBJECT_CLAIM_NAME);
        idp.setTrustEmail(true);

        try (Response response = realm.admin().identityProviders().create(idp)) {
            String body = response.hasEntity() ? response.readEntity(String.class) : "";
            assertEquals(201, response.getStatus(), body);
            assertTrue(response.getLocation() != null, "Missing identity provider location");
        }

        IdentityProviderRepresentation created = realm.admin().identityProviders().get(IDP_ALIAS).toRepresentation();
        assertNotNull(created, "Missing created identity provider");
        assertEquals(IDP_ALIAS, created.getAlias());
    }

    private OID4VPBasicWallet setupWallet() {
        oauth.realm(realm.getName()).client(CLIENT_ID, CLIENT_SECRET);
        oauth.scope("openid profile");
        createIdentityProvider();
        return new OID4VPBasicWallet(oauth, loginPage, driver);
    }

    private AuthorizationRequest fetchAuthorizationRequest(OID4VPBasicWallet wallet) throws Exception {
        OID4VPBasicWallet.WalletAuthorizationRequest walletRequest = wallet.browserAuthorizationRequest(IDP_ALIAS);
        assertNotNull(walletRequest.getWalletUrl(), "No wallet URL");
        assertThat(walletRequest.getWalletUrl(), startsWith(WALLET_SCHEME));
        assertNotNull(walletRequest.getClientId(), "No wallet client_id");
        assertNotNull(walletRequest.getRequestUri(), "No request_uri");

        AuthorizationRequest authorizationRequest = wallet.fetchAuthorizationRequest(walletRequest);
        assertEquals(realm.getBaseUrl(), authorizationRequest.getIssuer());
        assertEquals(walletRequest.getClientId(), authorizationRequest.getClientId());
        return authorizationRequest;
    }

    private String vpTokenFor(AuthorizationRequest authorizationRequest, String presentation) {
        String credentialQueryId = authorizationRequest.getDcqlQuery().getCredentials().get(0).getId();
        return "{\"" + credentialQueryId + "\":[\"" + presentation + "\"]}";
    }

    private String createVerifiedCredential(AuthorizationRequest authorizationRequest) {
        String realmName = realm.getName();
        String audience = authorizationRequest.getResponseUri();
        String nonce = authorizationRequest.getNonce();
        return runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            KeyWrapper issuerKey = session.keys().getActiveKey(realm, KeyUse.SIG, Algorithm.RS256);
            KeyWrapper holderKey = createHolderKey();
            int now = Time.currentTime();

            ObjectNode claims = JsonSerialization.mapper.createObjectNode();
            claims.put(OID4VCConstants.CLAIM_NAME_ISSUER, "https://issuer.example.org");
            claims.put(SUBJECT_CLAIM_NAME, "alice");
            claims.put("given_name", "Alice");
            claims.put("family_name", "Doe");
            claims.put("email", "alice@example.org");
            claims.put("vct", "urn:keycloak:oid4vp:credential");

            IssuerSignedJWT issuerSignedJWT = IssuerSignedJWT.builder()
                    .withClaims(claims)
                    .withIat(now - 60)
                    .withNbf(now - 60)
                    .withExp(now + 600)
                    .withKeyBindingKey(JWKBuilder.create().ec(holderKey.getPublicKey()))
                    .withKid(issuerKey.getKid())
                    .build();

            KeyBindingJWT keyBindingJWT = KeyBindingJWT.builder()
                    .withIat(now)
                    .withAudience(audience)
                    .withNonce(nonce)
                    .build();

            return SdJwt.builder()
                    .withIssuerSignedJwt(issuerSignedJWT)
                    .withKeybindingJwt(keyBindingJWT)
                    .withIssuerSigningContext(KeyWrapperUtil.createSignatureSignerContext(issuerKey))
                    .withKeyBindingSigningContext(KeyWrapperUtil.createSignatureSignerContext(holderKey))
                    .withUseDefaultDecoys(false)
                    .build()
                    .toSdJwtString();
        }, String.class);
    }

    private static KeyWrapper createHolderKey() {
        KeyWrapper key = OID4VCProofTestUtils.createEcKeyPair("holder-" + UUID.randomUUID());
        key.setUse(KeyUse.SIG);
        key.setAlgorithm(Algorithm.ES256);
        return key;
    }
}
