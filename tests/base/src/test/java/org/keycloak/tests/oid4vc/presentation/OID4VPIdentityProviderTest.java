package org.keycloak.tests.oid4vc.presentation;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import jakarta.ws.rs.core.Response;

import org.keycloak.protocol.oid4vc.presentation.OID4VPIdentityProviderConfig;
import org.keycloak.protocol.oid4vc.presentation.OID4VPIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.protocol.oid4vc.presentation.OID4VPConstants;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.presentation.DirectPostResponse;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.apache.http.client.utils.URLEncodedUtils;
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

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient(config = OID4VPClientConfig.class)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectWebDriver
    ManagedWebDriver driver;

    @AfterEach
    void cleanupBrowser() {
        driver.cookies().deleteAll();
        driver.open("about:blank");
    }

    @Test
    public void testRequestObjectAndDirectPostEndpoints() throws Exception {
        oauth.realm(realm.getName()).client(CLIENT_ID, CLIENT_SECRET);
        oauth.scope("openid profile");
        createIdentityProvider();

        OID4VPBasicWallet wallet = new OID4VPBasicWallet(oauth, loginPage, driver);

        // The browser initiates the broker login and reaches the wallet redirect.
        OID4VPBasicWallet.WalletAuthorizationRequest walletRequest = wallet.browserAuthorizationRequest(IDP_ALIAS);
        assertNotNull(walletRequest.getWalletUrl(), "No wallet URL");
        assertThat(walletRequest.getWalletUrl(), startsWith(WALLET_SCHEME));
        assertNotNull(walletRequest.getClientId(), "No wallet client_id");
        assertNotNull(walletRequest.getRequestUri(), "No request_uri");

        // The wallet fetches the Request Object from the verifier.
        AuthorizationRequest authorizationRequest = wallet.fetchAuthorizationRequest(walletRequest);
        assertEquals(OID4VPConstants.RESPONSE_TYPE_VP_TOKEN, authorizationRequest.getResponseType());
        assertEquals(OID4VPConstants.RESPONSE_MODE_DIRECT_POST, authorizationRequest.getResponseMode());
        assertEquals(OID4VPConstants.AUD_SELF_ISSUED_V2, authorizationRequest.getAudience());
        assertEquals(realm.getBaseUrl(), authorizationRequest.getIssuer());
        assertEquals(walletRequest.getClientId(), authorizationRequest.getClientId());
        assertNotNull(authorizationRequest.getResponseUri(), "No response_uri");
        assertNotNull(authorizationRequest.getDcqlQuery(), "No DCQL query");
        assertEquals(1, authorizationRequest.getDcqlQuery().getCredentials().size());

        // The wallet sends a background direct_post response and receives a redirect URI for the browser.
        DirectPostResponse directPostResponse = wallet.submitDirectPost(authorizationRequest, "dummy-vp-token");
        assertNotNull(directPostResponse.getRedirectUri(), "No redirect_uri");
        assertThat(directPostResponse.getRedirectUri(), startsWith(realm.getBaseUrl() + "/broker/" + IDP_ALIAS + "/endpoint/continue"));
        assertEquals(authorizationRequest.getState(), getQueryParam(directPostResponse.getRedirectUri(), OID4VPConstants.STATE));
        assertNotNull(getQueryParam(directPostResponse.getRedirectUri(), OID4VPConstants.RESPONSE_CODE), "No response_code");
    }

    @Test
    public void testBrowserCanResumeBrokerLoginFromDirectPostRedirectUri() throws Exception {
        oauth.realm(realm.getName()).client(CLIENT_ID, CLIENT_SECRET);
        oauth.scope("openid profile");
        createIdentityProvider();

        OID4VPBasicWallet wallet = new OID4VPBasicWallet(oauth, loginPage, driver);

        // The browser initiates the broker login and receives the wallet URL.
        OID4VPBasicWallet.WalletAuthorizationRequest walletRequest = wallet.browserAuthorizationRequest(IDP_ALIAS);

        // The wallet processes the request object and performs a background direct_post callback.
        AuthorizationRequest authorizationRequest = wallet.fetchAuthorizationRequest(walletRequest);
        DirectPostResponse directPostResponse = wallet.submitDirectPost(authorizationRequest, "dummy-vp-token");

        // The browser resumes the login flow by following the redirect URI returned to the wallet.
        AuthorizationEndpointResponse authorizationResponse = wallet.continueInBrowser(directPostResponse);
        assertThat(driver.getCurrentUrl(), startsWith(oauth.getRedirectUri()));

        String code = authorizationResponse.getCode();
        assertNotNull(code, "No authorization code");
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code).send();
        assertTrue(tokenResponse.isSuccess(), tokenResponse.getErrorDescription());
    }

    private String getQueryParam(String uri, String name) {
        return URLEncodedUtils.parse(URI.create(uri), StandardCharsets.UTF_8).stream()
                .filter(param -> name.equals(param.getName()))
                .map(param -> param.getValue())
                .findFirst()
                .orElse(null);
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
}
