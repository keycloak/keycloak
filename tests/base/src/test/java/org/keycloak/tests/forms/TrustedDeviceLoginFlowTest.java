package org.keycloak.tests.forms;

import java.util.List;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.TermsAndConditionsPage;
import org.keycloak.testframework.ui.page.TrustedDeviceRegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.providers.forms.ClickThroughAuthenticator;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FlowUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = TrustedDeviceLoginFlowTest.ServerConfig.class)
public class TrustedDeviceLoginFlowTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser( config = TrustedDeviceLoginFlowTestUserConfig.class )
    ManagedUser user;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected TrustedDeviceRegisterPage trustDevicePage;

    @InjectPage
    protected TermsAndConditionsPage termsPage;

    @TestSetup
    public void setUpFlow() {
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        rep.setOtpPolicyCodeReusable(true);
        rep.setBrowserFlow(setBrowserFlow());
        managedRealm.admin().update(rep);
    }

    @AfterEach
    public void cleanUp() {
        events.clear();
        UserResource userResource = user.admin();
        List<CredentialRepresentation> credentials = userResource.credentials();
        for (CredentialRepresentation credential : credentials) {
            // clean up all trusted devices
            if (credential.getType().equals(TrustedDeviceCredentialModel.TYPE)) {
                userResource.removeCredential(credential.getId());
            }
        }
    }

    protected String setBrowserFlow() {
        final String newFlowAlias = "browser-trusted-device-flow";
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.REQUIRED, subflow -> subflow
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, "auth-username-password-form")
                                .addSubFlowExecution(AuthenticationExecutionModel.Requirement.REQUIRED, trustedOr2fa -> trustedOr2fa
                                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, "auth-trusted-device")
                                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, secondFactor -> secondFactor
                                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ClickThroughAuthenticator.PROVIDER_ID)
                                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, "auth-trusted-device-register")
                                        )
                                )
                        )
                )
                .defineAsBrowserFlow()
        );
        return newFlowAlias;
    }


    @Test
    public void testLoginWithoutCookieThenTrustDevice() {
        loginAndTrustDevice();
        logout();
    }


    @Test
    public void testLoginWithTrustedCookieBypasses2fa() {
        loginAndTrustDevice();
        logout();
        events.clear();

        oauth.openLoginForm();
        oauth.fillLoginForm(user.getUsername(), user.getPassword());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(user.getId())
                .details(Details.USERNAME, user.getUsername());
        logout();
    }

    @Test
    public void testLoginWithoutCookieAndRejectDevice() {
        loginAndDeclineDevice();
        logout();
        events.clear();

        loginAndDeclineDevice();
        logout();
    }

    private void loginAndTrustDevice() {
        loginWith2fa();

        trustDevicePage.assertCurrent();

        trustDevicePage.confirmDevice();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.UPDATE_CREDENTIAL)
                .userId(user.getId())
                .details(Details.CREDENTIAL_TYPE, "trusted-device");
    }

    private void loginAndDeclineDevice() {
        loginWith2fa();

        trustDevicePage.assertCurrent();

        trustDevicePage.rejectDevice();
        EventAssertion.assertError(events.poll())
                .type(EventType.UPDATE_CREDENTIAL_ERROR)
                .userId(user.getId())
                .details(Details.CREDENTIAL_TYPE, "trusted-device")
                .details(Details.REASON, "user_declined");
    }

    private void logout() {
        AccountHelper.logout(managedRealm.admin(), user.getUsername());
    }

    private void loginWith2fa() {
        oauth.openLoginForm();
        loginPage.assertCurrent();
        oauth.fillLoginForm(user.getUsername(), user.getPassword());

        termsPage.assertCurrent();
        termsPage.acceptTerms();
    }

    protected static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }

    private static class TrustedDeviceLoginFlowTestUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder config) {
            return config.username("user")
                    .password("password")
                    .name("Trust", "Device")
                    .email("trusted-device@test.com")
                    .emailVerified(true);
        }
    }
}
