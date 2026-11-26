package org.keycloak.tests.forms;

import org.keycloak.events.Details;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.RegisterPage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RegistrationPasswordPolicyTest {

    @InjectRealm(config = RegistrationRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    RegisterPage registerPage;

    @Test
    public void registerUserNotEmailPasswordPolicyWithUsername() {
        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // Password equals username but not email - It must succeed because notEmail compares against email
        registerPage.register("firstName", "lastName", "bob@example.com", "alice12345", "alice12345", "alice12345");
        assertTrue(oauth.parseLoginResponse().isSuccess());

        String userId = EventAssertion.expectRegisterSuccess(events.poll()).clientId(oauth.getClientId())
                .details(Details.USERNAME, "alice12345").details(Details.EMAIL, "bob@example.com").getEvent().getUserId();
        EventAssertion.expectLoginSuccess(events.poll()).details("username", "alice12345").userId(userId);
        managedRealm.admin().users().get(userId).remove();

        oauth.openLoginForm();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // Password equals email - It must fail because notEmail compares against email
        registerPage.register("firstName", "lastName", "bob@example.com", "alice12345", "bob@example.com", "bob@example.com");
        registerPage.assertCurrent();
        assertEquals("Invalid password: must not be equal to the email.", registerPage.getPasswordError());

        // Case-sensitivity - still must not allow password equal to email
        registerPage.register("firstName", "lastName", "bob@example.com", "alice12345", "BOB@EXAMPLE.COM", "BOB@EXAMPLE.COM");
        registerPage.assertCurrent();
        assertEquals("Invalid password: must not be equal to the email.", registerPage.getPasswordError());
    }

    public static class RegistrationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.registrationAllowed(true)
                    .registrationEmailAsUsername(false)
                    .passwordPolicy("notEmail");
        }
    }
}
