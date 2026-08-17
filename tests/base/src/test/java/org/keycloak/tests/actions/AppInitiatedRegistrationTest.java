package org.keycloak.tests.actions;

import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.tests.utils.LegacyRealmConfig;
import org.keycloak.testsuite.admin.AdminApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AppInitiatedRegistrationTest {

    @InjectRealm(config = AppInitiatedRegistrationRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    protected RegisterPage registerPage;

    @BeforeEach
    public void before() {
        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "test-user@localhost");
    }

    @Test
    public void ensureLocaleParameterIsPropagatedDuringAppInitiatedRegistration() {

        oauth.registrationForm()
                .param(LocaleSelectorProvider.KC_LOCALE_PARAM, "en")
                .open();

        registerPage.assertCurrent();
        registerPage.register("first", "last", "test-user@localhost", "test-user", "test","test");

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = managedRealm.admin().users().searchByEmail("test-user@localhost", true).get(0);
        // ensure that the locale was set on the user
        Assertions.assertEquals("en", user.getAttributes().get("locale").get(0));
    }

    private static class AppInitiatedRegistrationRealmConfig extends LegacyRealmConfig {

        @Override
        public void configureTestRealm(RealmRepresentation testRealm) {
        }
    }
}
