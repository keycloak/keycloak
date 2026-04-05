package org.keycloak.testsuite.forms;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.theme.ThemeSelectorProvider;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThemeSelectorTest extends AbstractTestRealmKeycloakTest {

    private static final String SYSTEM_DEFAULT_LOGIN_THEME = ThemeSelectorProvider.DEFAULT_V2;

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void clientOverride() {
        loginPage.open();
        assertEquals(System.getProperty(PROPERTY_LOGIN_THEME_DEFAULT, SYSTEM_DEFAULT_LOGIN_THEME), detectTheme());

        ClientRepresentation rep = testRealm().clients().findByClientId("test-app").get(0);

        try {
            rep.getAttributes().put("login_theme", "base");
            testRealm().clients().get(rep.getId()).update(rep);

            loginPage.open();
            assertEquals("base", detectTheme());

            // assign a theme that does not exist, should use the default keycloak
            rep.getAttributes().put("login_theme", "unavailable-theme");
            testRealm().clients().get(rep.getId()).update(rep);

            loginPage.open();
            assertEquals(SYSTEM_DEFAULT_LOGIN_THEME, detectTheme());
        } finally {
            rep.getAttributes().put("login_theme", "");
            testRealm().clients().get(rep.getId()).update(rep);
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
