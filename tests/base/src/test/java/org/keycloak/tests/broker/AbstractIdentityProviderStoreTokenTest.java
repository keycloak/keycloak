package org.keycloak.tests.broker;

import java.util.Optional;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
public abstract class AbstractIdentityProviderStoreTokenTest {

    public static String IDP_ALIAS = "token-idp-alias";

    public static String EXTERNAL_REALM_NAME = "external-realm";

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectOAuthClient(ref = "external-realm", realmRef = "external-realm", config = TestClientConfig.class)
    protected OAuthClient oauthExternal;

    @InjectPage
    protected LoginPage loginPage;

    @InjectRunOnServer
    protected RunOnServerClient runOnServer;

    protected abstract ManagedRealm getRealm();
    protected abstract ManagedRealm getExternalRealm();
    protected abstract void checkSuccessfulTokenResponse(AccessTokenResponse externalTokens);

    @AfterEach
    public void logout() {
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
    public void testOIDCIdentityProviderStoreTokenManualRoleGrant() {
        ManagedRealm realm = getRealm();
        realm.updateIdentityProvider(IDP_ALIAS, idp-> {
            idp.setAddReadTokenRoleOnCreate(false);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        //user without the role tries to read the stored token
        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        AccountHelper.logout(realm.admin(), "testuser");

        //grant the role to the user and repeat the login
        runOnServer.run(session -> {
            RealmModel r = session.getContext().getRealm();
            ClientModel brokerClient = r.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
            RoleModel readTokenRole = brokerClient.getRole(Constants.READ_TOKEN_ROLE);
            UserModel user = session.users().getUserByUsername(r, "testuser");
            user.grantRole(readTokenRole);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());
        Assertions.assertNotNull(externalTokens.getAccessToken());
        checkSuccessfulTokenResponse(externalTokens);
    }

    @Test
    public void testOIDCIdentityProviderStoreTokenRoleGrantOnUserCreation() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());
        checkSuccessfulTokenResponse(externalTokens);
    }

    @Test
    public void testOIDCIdentityProviderStoreTokenGrantViaClientSettings() {
        ManagedRealm realm = getRealm();
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setAddReadTokenRoleOnCreate(false);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(tokenResponse.isSuccess());

        //external access disabled
        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        //external access enabled but idp is not selected
        ClientResource clientResource = AdminApiUtil.findClientByClientId(realm.admin(), "test-app");
        ManagedClient client = new ManagedClient(clientResource.toRepresentation(), clientResource);
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString()));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        //external access disabled but idp selected
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        //external access enabled and idp selected
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS));
        externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, tokenResponse.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());
        checkSuccessfulTokenResponse(externalTokens);

        //restore attributes as cleanup for client is wip
        client.updateWithCleanup(c-> c.attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.FALSE.toString()).attribute(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, null));
    }

    @Test
    public void testStoreTokenDisabled() {
        ManagedRealm realm = getRealm();
        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setStoreToken(false);
        });

        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());
        checkSuccessfulTokenResponse(externalTokens);

        String realmName = realm.getName();
        String oldTokenFromDatabase = runOnServer.fetch(session -> {
            RealmModel r = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(r, "testuser");
            return session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID).getFederatedIdentity(r, user, IDP_ALIAS).getToken();
        }, String.class);

        //Ensure that the token is null in the db
        Assertions.assertNull(oldTokenFromDatabase);
    }

    @Test
    public void testIdpDisabled() {
        ManagedRealm realm = getRealm();
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        realm.updateIdentityProvider(IDP_ALIAS, idp -> {
            idp.setEnabled(false);
        });

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(502, externalTokens.getStatusCode());
    }

    @Test
    public void testUserDisabled() {
        ManagedRealm realm = getRealm();
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin("testuser", "password");
        loginPage.submit();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        realm.updateUser("testuser", user -> {
            user.setEnabled(false);
        });

        AccessTokenResponse externalTokens = oauth.doFetchExternalIdpToken(IDP_ALIAS, internalTokens.getAccessToken());
        Assertions.assertEquals(400, externalTokens.getStatusCode());
    }

    public static class TestClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId("test-app")
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("http://localhost:8080/*")
                    .secret("test-secret");
        }
    }
}
