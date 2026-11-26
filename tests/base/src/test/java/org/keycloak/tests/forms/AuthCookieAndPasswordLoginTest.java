package org.keycloak.tests.forms;

import java.time.Duration;

import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.AuthenticationExecutionExportBuilder;
import org.keycloak.testframework.realm.AuthenticationFlowBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.tests.common.BasicUserConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class AuthCookieAndPasswordLoginTest {

    @InjectRealm(config = AuthCookieAndPasswordLoginRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectUser(config = BasicUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser testUser;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    PasswordPage passwordPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void cookieAndPasswordLogin() throws Exception {
        // login normally using the username and password page
        oauth.doLogin(testUser.getUsername(), testUser.getPassword());
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        // using cookie and password
        oauth.openLoginForm();
        passwordPage.assertCurrent();
        Assertions.assertEquals(testUser.getUsername(), passwordPage.getAttemptedUsername());
        passwordPage.fillPassword(testUser.getPassword());
        passwordPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        runOnServer.run(InfinispanTimeUtil.enableTestingTimeService());
        try {
            // expire the login using 10 seconds for login
            managedRealm.updateWithCleanup(r -> r.accessCodeLifespanUserAction(10).accessCodeLifespanLogin(10));
            oauth.openLoginForm();
            passwordPage.assertCurrent();
            Assertions.assertEquals(testUser.getUsername(), passwordPage.getAttemptedUsername());
            timeOffSet.set(Duration.ofSeconds(70));
            passwordPage.fillPassword(testUser.getPassword());
            passwordPage.submit();
            Assertions.assertEquals("Your login attempt timed out. Login will start from the beginning.",
                    passwordPage.getErrorMessage().orElse(null));

            // login again should success
            passwordPage.fillPassword(testUser.getPassword());
            passwordPage.submit();
            Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        } finally {
            runOnServer.run(InfinispanTimeUtil.disableTestingTimeService());
        }
    }

    static class AuthCookieAndPasswordLoginRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name("test").authenticationFlows(
                    AuthenticationFlowBuilder.create("step-cookie", "SSO auth-cookie flow", "basic-flow", false, false)
                            .authenticationExecutions(
                                    AuthenticationExecutionExportBuilder.authenticator(CookieAuthenticatorFactory.PROVIDER_ID,
                                            AuthenticationExecutionModel.Requirement.REQUIRED.name(), 10, false),
                                    AuthenticationExecutionExportBuilder.authenticator(PasswordFormFactory.PROVIDER_ID,
                                            AuthenticationExecutionModel.Requirement.REQUIRED.name(), 20, false)),
                    AuthenticationFlowBuilder.create("step-forms", "Username Password flow", "basic-flow", false, false)
                            .authenticationExecutions(
                                    AuthenticationExecutionExportBuilder.authenticator(UsernamePasswordFormFactory.PROVIDER_ID,
                                            AuthenticationExecutionModel.Requirement.REQUIRED.name(), 10, false)),
                    AuthenticationFlowBuilder.create("cookie-password-browser", "Cookie and password browser", "basic-flow", true, false)
                            .authenticationExecutions(
                                    AuthenticationExecutionExportBuilder.alias("step-cookie",
                                            AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(), 10, false),
                                    AuthenticationExecutionExportBuilder.alias("step-forms",
                                            AuthenticationExecutionModel.Requirement.ALTERNATIVE.name(), 20, false)))
                    .browserFlow("cookie-password-browser");
        }
    }
}
