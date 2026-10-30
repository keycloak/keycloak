package org.keycloak.tests.broker;

import java.util.Optional;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
public interface InterfaceIdentityProviderStoreTokenTest {

    public static String IDP_ALIAS = "token-idp-alias";

    public static String EXTERNAL_REALM_NAME = "external-realm";

    ManagedRealm getRealm();
    ManagedRealm getExternalRealm();
    OAuthClient getOAuthClient();
    LoginPage getLoginPage();
    RunOnServerClient getRunOnServer();
    AbstractHttpResponse doFetchExternalIdpToken(String token);
    void checkSuccessfulTokenResponse(AbstractHttpResponse response);
    void assertPublicClientTokenResponse(AbstractHttpResponse response);
    boolean isIdentityBrokeringAPIV1();
    boolean isRefreshTokenAllowed();

    default void loginWithIdP() {
        LoginPage loginPage = getLoginPage();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();
    }

    @AfterEach
    default void logout() {
        ManagedRealm realm = getRealm();
        Optional<UserRepresentation> userResult = realm.admin().users().search("testuser", true).stream().findFirst();
        if (userResult.isPresent()) {
            AccountHelper.logout(realm.admin(), "testuser");
            realm.admin().users().delete(userResult.get().getId()).close();
        }

        ManagedRealm externalRealm = getExternalRealm();
        userResult = externalRealm.admin().users().search("testuser", true).stream().findFirst();
        if (userResult.isPresent()) {
            AccountHelper.logout(externalRealm.admin(), "testuser");
        }
    }

    @Test
    default void testPublicClient() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        ClientResource clientResource = oauth.clientResource();
        ClientRepresentation originalClient = clientResource.toRepresentation();
        realm.cleanup().add(r -> clientResource.update(originalClient));
        clientResource.update(ClientBuilder.update(clientResource.toRepresentation())
                .publicClient(true)
                .serviceAccountsEnabled(false)
                .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString())
                .attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS)
                .build());

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.accessTokenRequest(oauth.parseLoginResponse().getCode())
                .client(oauth.getClientId())
                .send();
        Assertions.assertTrue(internalTokens.isSuccess());

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        assertPublicClientTokenResponse(externalTokens);
    }

    @Test
    default void testDefaultSuccess() {
        OAuthClient oauth = getOAuthClient();
        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(tokenResponse.getAccessToken());
        Assertions.assertTrue(externalTokens.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);
    }

    @Test
    default void testIdpDisabled() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setEnabled(false);
        });

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertFalse(externalTokens.isSuccess());
    }

    @Test
    default void testUserDisabled() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        UserRepresentation userRep = AdminApiUtil.findUserByUsername(realm.admin(), "testuser");
        userRep.setEnabled(false);
        realm.admin().users().get(userRep.getId()).update(userRep);

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(400, externalTokens.getStatusCode());
    }
}
