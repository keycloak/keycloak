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
