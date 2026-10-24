package org.keycloak.testsuite.forms;

import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.theme.ThemeSelectorProvider;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThemeSelectorTest extends AbstractTestRealmKeycloakTest {

    private static final String SYSTEM_DEFAULT_LOGIN_THEME = ThemeSelectorProvider.DEFAULT_V2;

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void clientOverride() {
        oauth.openLoginForm();
        assertEquals(System.getProperty(PROPERTY_LOGIN_THEME_DEFAULT, SYSTEM_DEFAULT_LOGIN_THEME), detectTheme());

        ClientRepresentation rep = managedRealm.admin().clients().findByClientId("test-app").get(0);

        try {
            rep.getAttributes().put("login_theme", "base");
            managedRealm.admin().clients().get(rep.getId()).update(rep);

            oauth.openLoginForm();
            assertEquals("base", detectTheme());

            // assign a theme that does not exist, should use the default keycloak
            rep.getAttributes().put("login_theme", "unavailable-theme");
            managedRealm.admin().clients().get(rep.getId()).update(rep);

            oauth.openLoginForm();
            assertEquals(SYSTEM_DEFAULT_LOGIN_THEME, detectTheme());
        } finally {
            rep.getAttributes().put("login_theme", "");
            managedRealm.admin().clients().get(rep.getId()).update(rep);
        }
    }

    @Test
    public void realmDisplayNamePlaceholderInTitleResolvedFromRealmLocalizationTexts() {
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        String originalDisplayName = rep.getDisplayName();
        try {
            managedRealm.admin().localization().createOrUpdateRealmLocalizationTexts("en",
                    Map.of("myRealmDisplayNameKey", "My browser tab title"));
            rep.setDisplayName("${myRealmDisplayNameKey}");
            managedRealm.admin().update(rep);

            oauth.openLoginForm();
            assertEquals("Sign in to My browser tab title", driver.getTitle());

            // a display name without placeholders is rendered as is
            rep.setDisplayName("Test Realm");
            managedRealm.admin().update(rep);
            oauth.openLoginForm();
            assertEquals("Sign in to Test Realm", driver.getTitle());
        } finally {
            rep.setDisplayName(originalDisplayName);
            managedRealm.admin().update(rep);
            managedRealm.admin().localization().deleteRealmLocalizationText("en", "myRealmDisplayNameKey");
        }
    }

    private String detectTheme() {
        // for the purpose of the test does not matter which profile is used (product or community)
        if(driver.getPageSource().contains("/login/keycloak/css/login.css") || driver.getPageSource().contains("/login/rh-sso/css/login.css")) {
            return "keycloak";
        } else if (driver.getPageSource().contains("/login/keycloak.v2/css/styles.css") || driver.getPageSource().contains("/login/rh-sso/css/styles.css")) {
            return "keycloak.v2";
        } else {
            return "base";
        }
    }

}
