package org.keycloak.tests.broker;

import java.util.List;
import java.util.Map;

import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A second browser tab opened while a login is in progress shares the root authentication session. When that tab
 * then authenticates through an already linked identity provider with prompt=login, no browser flow execution
 * completes, and the authentication lands in the user session and client session the first tab created. The amr
 * claim of the second tab's tokens must not report the password step the first tab performed.
 */
@KeycloakIntegrationTest
public class BrokerSecondTabAmrTest extends AbstractKcOidcBrokerTest {

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void secondTabBrokeredLoginIntoExistingSession() {
        disableUpdateProfileOnFirstLogin();

        prepareLinkedUser();

        addAmrMapper();
        setPasswordReference(0);

        // tab 1: plain login form; tab 2: forced login straight to the identity provider, same root auth session
        oauth.openLoginForm();
        webDriver.tabs().newTab(oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).param("kc_idp_hint", IDP_OIDC_ALIAS).build());
        Assertions.assertEquals(2, webDriver.tabs().getCountOfTabs());

        webDriver.tabs().switchToTab(0);
        oauth.fillLoginForm(USER_LOGIN, USER_PASSWORD);
        AccessTokenResponse first = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertEquals(200, first.getStatusCode());
        String sid1 = oauth.parseRefreshToken(first.getRefreshToken()).getSessionId();
        Assertions.assertEquals(List.of("password"), amr(first));

        timeOffSet.set(5);

        webDriver.tabs().switchToTab(1);
        oauth.fillLoginForm(USER_LOGIN, USER_PASSWORD); // the identity provider's login page
        AccessTokenResponse second = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertEquals(200, second.getStatusCode());
        String sid2 = oauth.parseRefreshToken(second.getRefreshToken()).getSessionId();

        Assertions.assertEquals(sid1, sid2, "second tab is expected to land in the first tab's user session");
        Assertions.assertEquals(List.of(), amr(second), "the brokered login completed no password step, so the first tab's password reference must not be reported");
    }

    /**
     * Offline variant. The first offline token of a brand new user session removes the online user session and the
     * root authentication session (OAuth2GrantTypeBase), so a concurrent tab cannot join a session whose first token
     * was offline. The offline copy of a client session is only reused when the user session already existed: tab 1
     * logs in online, tab 2 logs in with a password and offline_access into that session and creates the offline copy,
     * tab 3 logs in through the identity provider with offline_access into the same session and reuses that copy.
     * A refresh of tab 3's offline token must not report tab 2's password step.
     */
    @Test
    public void thirdTabBrokeredOfflineLoginReusingOfflineSession() {
        disableUpdateProfileOnFirstLogin();
        prepareLinkedUser();
        addAmrMapper();
        setPasswordReference(0);

        oauth.openLoginForm();
        oauth.scope(org.keycloak.OAuth2Constants.OFFLINE_ACCESS);
        webDriver.tabs().newTab(oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).build());
        webDriver.tabs().newTab(oauth.loginForm().prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN).param("kc_idp_hint", IDP_OIDC_ALIAS).build());
        Assertions.assertEquals(3, webDriver.tabs().getCountOfTabs());

        // tab 1: online password login creates the user session
        webDriver.tabs().switchToTab(0);
        oauth.scope(null);
        oauth.fillLoginForm(USER_LOGIN, USER_PASSWORD);
        AccessTokenResponse first = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertEquals(200, first.getStatusCode());
        String sid = oauth.parseRefreshToken(first.getRefreshToken()).getSessionId();

        // tab 2: forced password login with offline_access into the same session creates the offline copy with the password step
        timeOffSet.set(5);
        oauth.scope(org.keycloak.OAuth2Constants.OFFLINE_ACCESS);
        webDriver.tabs().switchToTab(1);
        oauth.fillLoginForm(USER_LOGIN, USER_PASSWORD);
        AccessTokenResponse second = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertEquals(200, second.getStatusCode());
        Assertions.assertEquals(sid, oauth.parseRefreshToken(second.getRefreshToken()).getSessionId());
        Assertions.assertEquals(List.of("password"), amr(second));

        // tab 3: brokered login with offline_access into the same session reuses the offline copy
        timeOffSet.set(10);
        webDriver.tabs().switchToTab(2);
        oauth.fillLoginForm(USER_LOGIN, USER_PASSWORD);
        AccessTokenResponse third = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        Assertions.assertEquals(200, third.getStatusCode());
        Assertions.assertEquals(sid, oauth.parseRefreshToken(third.getRefreshToken()).getSessionId());
        Assertions.assertEquals(List.of(), amr(third));

        timeOffSet.set(15);
        AccessTokenResponse refreshed = oauth.doRefreshTokenRequest(third.getRefreshToken());
        Assertions.assertEquals(200, refreshed.getStatusCode());
        Assertions.assertEquals(List.of(), amr(refreshed),
                "a refresh of the third tab's offline token must not restore the second tab's password reference");
    }

    private void prepareLinkedUser() {
        String providerUserId = AdminApiUtil.findUserByUsername(providerRealm.admin(), USER_LOGIN).getId();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(USER_LOGIN);
        user.setEmail(USER_EMAIL);
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setFirstName("First");
        user.setLastName("Last");
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(USER_PASSWORD);
        password.setTemporary(false);
        user.setCredentials(List.of(password));
        consumerRealm.admin().users().create(user).close();
        String consumerUserId = AdminApiUtil.findUserByUsername(consumerRealm.admin(), USER_LOGIN).getId();

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setIdentityProvider(IDP_OIDC_ALIAS);
        link.setUserId(providerUserId);
        link.setUserName(USER_LOGIN);
        consumerRealm.admin().users().get(consumerUserId).addFederatedIdentity(IDP_OIDC_ALIAS, link).close();
    }

    @SuppressWarnings("unchecked")
    private List<String> amr(AccessTokenResponse response) {
        Object amr = oauth.verifyToken(response.getAccessToken()).getOtherClaims().get("amr");
        return amr == null ? List.of() : (List<String>) amr;
    }

    private void addAmrMapper() {
        String clientUuid = consumerRealm.admin().clients().findByClientId(CONSUMER_BROKER_APP_CLIENT_ID).get(0).getId();
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("amr");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-amr-mapper");
        mapper.setConfig(Map.of("id.token.claim", "true", "access.token.claim", "true"));
        consumerRealm.admin().clients().get(clientUuid).getProtocolMappers().createMapper(mapper).close();
    }

    private void setPasswordReference(int maxAge) {
        var execution = consumerRealm.admin().flows().getExecutions("browser").stream()
                .filter(e -> "auth-username-password-form".equals(e.getProviderId())).findFirst().orElseThrow();
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias("password-reference");
        config.setConfig(Map.of(
                Constants.AUTHENTICATION_EXECUTION_REFERENCE_VALUE, "password",
                Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE, String.valueOf(maxAge)));
        consumerRealm.admin().flows().newExecutionConfig(execution.getId(), config).close();
    }
}
