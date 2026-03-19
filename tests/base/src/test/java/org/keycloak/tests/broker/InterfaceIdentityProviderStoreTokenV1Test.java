package org.keycloak.tests.broker;

import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
public interface InterfaceIdentityProviderStoreTokenV1Test extends InterfaceIdentityProviderStoreTokenTest {

    @Override
    default boolean isIdentityBrokeringAPIV1() {
        return true;
    }

    @Test
    default void testOIDCIdentityProviderStoreTokenManualRoleGrant() {
        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();
        realm.updateIdentityProvider(IDP_ALIAS, idp-> {
            idp.setAddReadTokenRoleOnCreate(false);
        });

        oauth.openLoginForm();
        loginWithIdP();

        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        //user without the role tries to read the stored token
        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode());

        AccountHelper.logout(realm.admin(), "testuser");

        //grant the role to the user and repeat the login
        getRunOnServer().run(session -> {
            RealmModel r = session.getContext().getRealm();
            ClientModel brokerClient = r.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID);
            RoleModel readTokenRole = brokerClient.getRole(Constants.READ_TOKEN_ROLE);
            UserModel user = session.users().getUserByUsername(r, "testuser");
            user.grantRole(readTokenRole);
        });

        oauth.openLoginForm();
        loginWithIdP();

        internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(200, externalTokens.getStatusCode());
        checkSuccessfulTokenResponse(externalTokens);
    }
}
