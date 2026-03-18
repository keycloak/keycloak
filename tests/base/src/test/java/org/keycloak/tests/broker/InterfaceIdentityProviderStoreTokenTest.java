package org.keycloak.tests.broker;

import java.util.Optional;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.AccountHelper;
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
    void checkSuccessfulTokenResponse(AccessTokenResponse externalTokens);
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
    default void testDefaultSuccess() {
        OAuthClient oauth = getOAuthClient();
        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
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

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(502, externalTokens.getStatusCode());
    }

    @Test
    default void testUserDisabled() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        realm.updateUser("testuser", user -> {
            user.setEnabled(false);
        });

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(400, externalTokens.getStatusCode());
    }
}
