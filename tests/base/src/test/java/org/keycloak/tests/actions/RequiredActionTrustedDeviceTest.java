package org.keycloak.tests.actions;

import org.keycloak.authentication.requiredactions.TrustedDeviceRegister;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;
import org.keycloak.representations.TrustedDeviceToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
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
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.TrustedDeviceRegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest
public class RequiredActionTrustedDeviceTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(config = RequiredActionTrustedDeviceTestUserConfig.class)
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

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @BeforeEach
    public void setupRequiredAction() {
        UserRepresentation userRepresentation = user.admin().toRepresentation();

        if (!userRepresentation.getRequiredActions().contains(TrustedDeviceRegister.PROVIDER_ID)) {
            userRepresentation.getRequiredActions().add(TrustedDeviceRegister.PROVIDER_ID);
        }

        user.admin().update(userRepresentation);
    }

    @Test
    public void declineTrustedDevice() {
        oauth.openLoginForm();

        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        trustDevicePage.assertCurrent();

        trustDevicePage.rejectDevice();

        EventRepresentation credentialEvent = events.poll();

        EventAssertion.assertError(credentialEvent)
                .type(EventType.UPDATE_CREDENTIAL_ERROR)
                .userId(user.getId())
                .details(Details.CREDENTIAL_TYPE, TrustedDeviceCredentialModel.TYPE)
                .details(Details.REASON, "user_declined")
                .details(Details.USERNAME, user.getUsername());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .userId(user.getId())
                .details(Details.USERNAME, user.getUsername());

        oauth.parseLoginResponse().getCode();
        driver.open(keycloakUrls.getBaseUrl() + "/realms/" + oauth.getRealm() + "/account");

        Cookie trustedDeviceCookie = driver.cookies().get(CookieType.TRUSTED_DEVICE);
        assertThat("KEYCLOAK_TRUSTED_DEVICE cookie should not be set", trustedDeviceCookie, nullValue());
        AccountHelper.logout(managedRealm.admin(), user.getUsername());
    }

    @Test
    public void confirmTrustedDevice() {
        oauth.openLoginForm();

        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        trustDevicePage.assertCurrent();
        trustDevicePage.confirmDevice();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.UPDATE_CREDENTIAL)
                .userId(user.getId())
                .details(Details.CREDENTIAL_TYPE, TrustedDeviceCredentialModel.TYPE)
                .details(Details.USERNAME, user.getUsername());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(user.getId())
                .details(Details.USERNAME, user.getUsername());

        oauth.parseLoginResponse().getCode();
        driver.open(keycloakUrls.getBaseUrl() + "/realms/" + oauth.getRealm() + "/account");

        Cookie trustedDeviceCookie = driver.cookies().get(CookieType.TRUSTED_DEVICE);
        assertThat("KEYCLOAK_TRUSTED_DEVICE cookie should be set", trustedDeviceCookie, notNullValue());

//        TrustedDeviceToken decoded = oauth.verifyToken(trustedDeviceCookie.getValue(), TrustedDeviceToken.class);
//        assertThat("Token should be decoded successfully", decoded, notNullValue());

        final String realmId = managedRealm.getName();
        final String tokenString = trustedDeviceCookie.getValue();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            assertThat("Token in cookie should be a set", tokenString, notNullValue());
            TrustedDeviceToken decoded = session.tokens().decode(tokenString, TrustedDeviceToken.class);
            assertThat("Token should be decoded successfully", decoded, notNullValue());
        });
    }

    private static class RequiredActionTrustedDeviceTestUserConfig implements UserConfig {

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
