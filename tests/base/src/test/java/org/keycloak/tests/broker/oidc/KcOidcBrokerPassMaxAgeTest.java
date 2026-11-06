package org.keycloak.tests.broker.oidc;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.broker.oidc.TestKeycloakOidcIdentityProviderFactory;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KcOidcBrokerPassMaxAgeTest extends AbstractKcOidcBrokerTest {

    private static final int MAX_AUTH_AGE = 30;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = PassMaxAgeConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    LoginPasswordUpdatePage changePasswordPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    // The standard login flow doesn't apply cleanly to this config (no client-forwarded prompt, custom
    // provider factory) - legacy disabled the same inherited test for this exact configuration and
    // overrode the other with its own max_age-specific logic instead. Same shape here.
    @Override
    @Test
    @Disabled("PassMaxAge config needs its own login flow; see testLoginWithExistingUser() below")
    public void testLogInAsUserInIDP() {
    }

    @Override
    @Test
    public void testLoginWithExistingUser() {
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");
        logoutFromConsumerRealm();
        AccountHelper.logout(providerRealm.admin(), getUserLogin());
        oauth.openLoginForm();
        loginPage.assertCurrent();

        logInWithBroker();

        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        timeOffSet.set(2);

        oauth.loginForm().maxAge(1).open();

        loginPage.assertCurrent();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/protocol/openid-connect/auth"),
                "Driver should be on the consumer realm page right now");

        logInWithBroker();

        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                "Driver should be on the provider realm page right now");
        loginPage.assertCurrent();

        loginPage.fillPassword(getUserPassword());
        loginPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    void testEnforceReAuthenticationWhenMaxAgeIsSet() {
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");
        logoutFromConsumerRealm();
        AccountHelper.logout(providerRealm.admin(), getUserLogin());
        oauth.openLoginForm();
        loginPage.assertCurrent();

        logInWithBroker();

        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        IdentityProviderRepresentation idpRep = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        TestKeycloakOidcIdentityProviderFactory.setIgnoreMaxAgeParam(idpRep);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idpRep);

        timeOffSet.set(2);

        oauth.loginForm().maxAge(1).open();

        loginPage.assertCurrent();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/protocol/openid-connect/auth"),
                "Driver should be on the consumer realm page right now");

        logInWithBroker();

        errorPage.assertCurrent();
        Assertions.assertEquals("Unexpected error when authenticating with identity provider",
                errorPage.getError());
    }

    @Test
    void testNoRejectionWhenMaxAgeIsIntegerMaxValue() {
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");
        logoutFromConsumerRealm();
        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        // max_age close to Integer.MAX_VALUE must not overflow the authTime+maxAge sum on the
        // consumer side and incorrectly reject a genuinely fresh authentication at the provider
        oauth.loginForm().maxAge(Integer.MAX_VALUE).open();
        loginPage.assertCurrent();

        logInWithBroker();

        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(),
                "Should be logged in; a huge max_age must not overflow and wrongly reject the response");
    }

    @Test
    void kcActionRequiresReAuthenticationWhenUpstreamLoginIsOlderThanMaxAuthAge() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), MAX_AUTH_AGE);
        createUserInConsumerRealmAndLogoutFromConsumerOnly();

        // The upstream (provider realm) session stays alive and becomes older than max_auth_age of the action
        timeOffSet.set(MAX_AUTH_AGE + 10);

        // The broker callback is recent, but the upstream login it reuses (SSO at the provider realm) is not
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        loginPage.assertCurrent();
        logInWithBroker();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");

        // The action must not be committed. The user has to authenticate again
        loginPage.assertCurrent();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/"),
                "Driver should be back on the consumer realm page");
    }

    @Test
    void kcActionSucceedsWhenUpstreamLoginIsFresh() {
        setMaxAuthAge(UserModel.RequiredAction.UPDATE_PASSWORD.name(), MAX_AUTH_AGE);
        createUserInConsumerRealmAndLogoutFromConsumerOnly();
        AccountHelper.logout(providerRealm.admin(), getUserLogin());

        // Time passes, but the user authenticates upstream again, so the upstream login is fresh
        timeOffSet.set(MAX_AUTH_AGE + 10);

        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_PASSWORD.name()).open();
        loginPage.assertCurrent();
        logInWithBroker();
        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("New-password1!", "New-password1!");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    private void createUserInConsumerRealmAndLogoutFromConsumerOnly() {
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");

        // Remove the consumer user sessions directly. The admin logout would also log the user out of the provider realm
        // (backchannel logout of the IdP), but the upstream SSO session has to stay alive for these tests.
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(CONSUMER_REALM);
            session.getContext().setRealm(realm);
            UserModel user = session.users().getUserByUsername(realm, USER_LOGIN);
            session.sessions().removeUserSessions(realm, user);
        });
    }

    private void setMaxAuthAge(String providerId, int maxAuthAge) {
        RequiredActionProviderRepresentation action = consumerRealm.admin().flows().getRequiredActions().stream()
                .filter(a -> providerId.equals(a.getProviderId()))
                .findFirst()
                .orElseThrow();
        Map<String, String> config = action.getConfig() == null ? new HashMap<>() : new HashMap<>(action.getConfig());
        config.put(Constants.MAX_AUTH_AGE_KEY, String.valueOf(maxAuthAge));
        action.setConfig(config);
        consumerRealm.admin().flows().updateRequiredAction(action.getAlias(), action);
    }

    static class PassMaxAgeConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .providerId(TestKeycloakOidcIdentityProviderFactory.ID)
                            .attribute(IdentityProviderModel.LOGIN_HINT, "false")
                            .attribute(IdentityProviderModel.PASS_MAX_AGE, "true")
                            // Legacy KcOidcBrokerConfigurationWithPassMaxAge removed the "prompt" attribute so the
                            // broker does not force an interactive login; without this the provider always shows its
                            // login page and the ignore-max-age error path is never exercised.
                            .attribute("prompt", null));
        }
    }
}
