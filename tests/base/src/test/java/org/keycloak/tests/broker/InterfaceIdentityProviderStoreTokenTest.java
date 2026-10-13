package org.keycloak.tests.broker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
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

    @Test
    default void testDcrClientCannotSelfGrantExternalTokenAccess() throws ClientRegistrationException {
        // TODO remove the assumption once https://github.com/keycloak/keycloak/pull/53752 is merged. Until then V1 only
        Assumptions.assumeFalse(isIdentityBrokeringAPIV1(), "Identity Brokering API V1 does not check the external token client attributes yet");

        ManagedRealm realm = getRealm();
        OAuthClient oauth = getOAuthClient();

        String initialAccessToken = realm.admin().clientInitialAccess()
                .create(new ClientInitialAccessCreatePresentation(0, 1)).getToken();
        ClientRegistration reg = oauth.clientRegistration();
        reg.auth(Auth.token(initialAccessToken));

        ClientRepresentation dcrClient = new ClientRepresentation();
        dcrClient.setClientId("dcr-client");
        dcrClient.setSecret("dcr-secret");
        dcrClient.setRedirectUris(Collections.singletonList(oauth.getRedirectUri()));
        dcrClient.setAttributes(externalTokenAttributes());
        dcrClient = reg.create(dcrClient);
        String dcrClientUuid = dcrClient.getId();
        realm.cleanup().add(r -> r.clients().get(dcrClientUuid).remove());
        realm.cleanup().add(r -> oauth.client("test-app", "test-secret"));

        oauth.client("dcr-client", "dcr-secret");
        oauth.openLoginForm();
        loginWithIdP();
        AccessTokenResponse internalTokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertTrue(internalTokens.isSuccess());

        AbstractHttpResponse externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode(),
                "Client must not be allowed to retrieve external tokens based on attributes set through dynamic client registration");

        reg.auth(Auth.token(dcrClient.getRegistrationAccessToken()));
        ClientRepresentation update = new ClientRepresentation();
        update.setClientId("dcr-client");
        update.setAttributes(externalTokenAttributes());
        reg.update(update);
        externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertEquals(403, externalTokens.getStatusCode(),
                "Client must not be allowed to retrieve external tokens based on attributes set through the registration access token");

        ClientResource clientResource = realm.admin().clients().get(dcrClientUuid);
        ClientRepresentation adminUpdate = clientResource.toRepresentation();
        adminUpdate.getAttributes().putAll(externalTokenAttributes());
        clientResource.update(adminUpdate);
        externalTokens = doFetchExternalIdpToken(internalTokens.getAccessToken());
        Assertions.assertTrue(externalTokens.isSuccess());
        checkSuccessfulTokenResponse(externalTokens);
    }

    static Map<String, String> externalTokenAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(OIDCConfigAttributes.EXTERNAL_TOKEN_ENABLED, Boolean.TRUE.toString());
        attributes.put(OIDCConfigAttributes.EXTERNAL_TOKEN_IDP, IDP_ALIAS);
        return attributes;
    }
}
