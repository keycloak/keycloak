package org.keycloak.tests.broker.oidc;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.broker.provider.IdpLinkAction;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.IdpLinkActionPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The {@code max_auth_age} check of the {@code idp_link} application-initiated action must not be evaluated after the
 * identity provider link was already persisted. The round trip to the external IdP can outlast {@code max_auth_age}.
 */
@KeycloakIntegrationTest
public class KcOidcBrokerIdpLinkActionMaxAuthAgeTest extends AbstractKcOidcBrokerTest {

    private static final String LOCAL_USER = "link-user";
    private static final String LOCAL_USER_PASSWORD = "link-user-password";
    private static final int MAX_AUTH_AGE = 30;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = IdpLinkConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectPage
    IdpLinkActionPage idpLinkActionPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @BeforeEach
    void configureMaxAuthAgeAndLoginLocally() {
        setIdpLinkMaxAuthAge(MAX_AUTH_AGE);

        oauth.openLoginForm();
        loginPage.fillLogin(LOCAL_USER, LOCAL_USER_PASSWORD);
        loginPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        assertNumFederatedIdentities(LOCAL_USER, 0);
    }

    @AfterEach
    void resetTimeOffset() {
        timeOffSet.set(0);
    }

    // The inherited first-broker-login scenarios do not apply here: every test starts with a local login
    // and links the IdP to that existing user through the idp_link action instead.
    @Override
    @Test
    @Disabled("Not applicable: tests of this class link the IdP to an already authenticated local user")
    public void testLogInAsUserInIDP() {
    }

    @Override
    @Test
    @Disabled("Not applicable: tests of this class link the IdP to an already authenticated local user")
    public void testLoginWithExistingUser() {
    }

    @Test
    void linkIsNotCommittedWhenIdpRoundTripOutlastsMaxAuthAge() {
        oauth.loginForm().kcAction(IdpLinkAction.PROVIDER_ID + ":" + getIdpAlias()).open();

        // The action is confirmed while the local authentication is still fresh, so the user is sent to the external IdP
        idpLinkActionPage.assertCurrent();
        idpLinkActionPage.confirm();
        loginPage.assertCurrent();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                "Driver should be on the provider realm page right now");

        // The login at the external IdP takes longer than max_auth_age
        timeOffSet.set(MAX_AUTH_AGE + 5);
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        // The action is rejected and the user must authenticate again ...
        loginPage.assertCurrent();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"),
                "Driver should be back on the consumer realm page");
        Assertions.assertFalse(webDriver.getCurrentUrl().contains("kc_action_status"),
                "The action must not report a status when it was rejected");

        // ... and the identity provider link must not be left committed
        assertNumFederatedIdentities(LOCAL_USER, 0);
    }

    @Test
    void linkIsCommittedWhenIdpRoundTripStaysWithinMaxAuthAge() {
        oauth.loginForm().kcAction(IdpLinkAction.PROVIDER_ID + ":" + getIdpAlias()).open();

        idpLinkActionPage.assertCurrent();
        idpLinkActionPage.confirm();
        loginPage.assertCurrent();

        timeOffSet.set(MAX_AUTH_AGE - 20);
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("success", response.getKcActionStatus());

        assertNumFederatedIdentities(LOCAL_USER, 1);
    }

    private void setIdpLinkMaxAuthAge(int maxAuthAge) {
        RequiredActionProviderRepresentation action = consumerRealm.admin().flows().getRequiredActions().stream()
                .filter(a -> IdpLinkAction.PROVIDER_ID.equals(a.getProviderId()))
                .findFirst()
                .orElseThrow();
        Map<String, String> config = action.getConfig() == null ? new HashMap<>() : new HashMap<>(action.getConfig());
        config.put(Constants.MAX_AUTH_AGE_KEY, String.valueOf(maxAuthAge));
        action.setConfig(config);
        consumerRealm.admin().flows().updateRequiredAction(action.getAlias(), action);
    }

    static class IdpLinkConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm, createOidcIdentityProvider())
                    // Users imported with the realm do not get the default roles, but idp_link requires manage-account(-links)
                    .users(UserBuilder.create(LOCAL_USER)
                            .email("link-user@localhost")
                            .name("Link", "User")
                            .emailVerified(true)
                            .password(LOCAL_USER_PASSWORD)
                            .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, AccountRoles.MANAGE_ACCOUNT)
                            .enabled(true));
        }
    }
}
