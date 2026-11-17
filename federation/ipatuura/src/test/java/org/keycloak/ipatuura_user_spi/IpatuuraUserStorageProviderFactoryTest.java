package org.keycloak.ipatuura_user_spi;

import java.util.Set;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.ComponentResource;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link IpatuuraUserStorageProviderFactory#validateConfiguration} itself, not just the
 * {@code requiresPasswordReentry} comparison helper, so a regression that skips the guard or moves it
 * after the outbound request is caught: in that case these tests would observe a "Cannot connect to
 * provided URL!" failure instead of the expected re-entry rejection.
 */
public class IpatuuraUserStorageProviderFactoryTest {

    private static final String OLD_URL = "server.example.com:8080";
    private static final String NEW_URL = "evil.example.com:8080";
    private static final String USERNAME = "admin";
    private static final String REENTRY_MESSAGE =
            "The login password must be re-entered when the Ipatuura Server URL or login username is changed";

    private final IpatuuraUserStorageProviderFactory factory = new IpatuuraUserStorageProviderFactory();

    private static ComponentModel component(String id, String url, String username) {
        ComponentModel model = new ComponentModel();
        model.setId(id);
        model.getConfig().putSingle("scimurl", url);
        model.getConfig().putSingle("loginusername", username);
        return model;
    }

    @SuppressWarnings("unchecked")
    private static KeycloakSession sessionWithPlaceholderPassword(boolean passwordIsPlaceholder) {
        KeycloakSession session = mock(KeycloakSession.class);
        Set<String> placeholderFields = passwordIsPlaceholder ? Set.of("loginpassword") : Set.of();
        when(session.getAttribute(ComponentResource.SECRET_PLACEHOLDER_FIELDS_ATTR, Set.class))
                .thenReturn(placeholderFields);
        return session;
    }

    private static RealmModel realmWithStoredComponent(ComponentModel stored) {
        RealmModel realm = mock(RealmModel.class);
        when(realm.getComponent(stored.getId())).thenReturn(stored);
        return realm;
    }

    @Test
    public void testValidateConfigurationRejectsUrlChangeWithPlaceholderPassword() {
        ComponentModel oldComponent = component("id1", OLD_URL, USERNAME);
        ComponentModel newComponent = component("id1", NEW_URL, USERNAME);
        KeycloakSession session = sessionWithPlaceholderPassword(true);
        RealmModel realm = realmWithStoredComponent(oldComponent);

        ComponentValidationException ex = Assert.assertThrows(ComponentValidationException.class,
                () -> factory.validateConfiguration(session, realm, newComponent));
        Assert.assertEquals(REENTRY_MESSAGE, ex.getMessage());
    }

    @Test
    public void testValidateConfigurationRejectsUsernameChangeWithPlaceholderPassword() {
        ComponentModel oldComponent = component("id1", OLD_URL, USERNAME);
        ComponentModel newComponent = component("id1", OLD_URL, "someoneelse");
        KeycloakSession session = sessionWithPlaceholderPassword(true);
        RealmModel realm = realmWithStoredComponent(oldComponent);

        ComponentValidationException ex = Assert.assertThrows(ComponentValidationException.class,
                () -> factory.validateConfiguration(session, realm, newComponent));
        Assert.assertEquals(REENTRY_MESSAGE, ex.getMessage());
    }

    @Test
    public void testValidateConfigurationAllowsUrlChangeWhenPasswordExplicitlyProvided() {
        ComponentModel oldComponent = component("id1", OLD_URL, USERNAME);
        ComponentModel newComponent = component("id1", NEW_URL, USERNAME);
        // password was re-entered, so it is not in the placeholder set
        KeycloakSession session = sessionWithPlaceholderPassword(false);
        RealmModel realm = realmWithStoredComponent(oldComponent);

        // the guard must let this through; the failure below comes from the (expected) outbound
        // connection attempt, proving validation actually progressed past the re-entry check
        ComponentValidationException ex = Assert.assertThrows(ComponentValidationException.class,
                () -> factory.validateConfiguration(session, realm, newComponent));
        Assert.assertEquals("Cannot connect to provided URL!", ex.getMessage());
    }

    @Test
    public void testValidateConfigurationSkipsGuardWhenCreatingNewComponent() {
        ComponentModel newComponent = component(null, NEW_URL, USERNAME);
        KeycloakSession session = sessionWithPlaceholderPassword(true);
        RealmModel realm = mock(RealmModel.class);

        // no stored component to compare against on creation; must reach the outbound connection attempt
        ComponentValidationException ex = Assert.assertThrows(ComponentValidationException.class,
                () -> factory.validateConfiguration(session, realm, newComponent));
        Assert.assertEquals("Cannot connect to provided URL!", ex.getMessage());
    }
}
