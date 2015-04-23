package org.keycloak.testsuite.ui.test.settings;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.model.Theme;
import org.keycloak.testsuite.ui.page.settings.ThemesSettingsPage;


/**
 * Created by fkiss.
 */
public class ThemesSettingsTest extends AbstractKeyCloakTest<ThemesSettingsPage> {

    @Before
    public void beforeThemeTest() {
        navigation.themes();
    }

    @Test
    public void changeLoginThemeTest() {
        page.changeLoginTheme(Theme.BASE.getName());
        page.saveTheme();
        logOut();
        page.verifyBaseTheme();

        loginAsAdmin();
        navigation.themes();
        page.changeLoginTheme(Theme.PATTERNFLY.getName());
        page.saveTheme();
        logOut();
        page.verifyPatternflyTheme();

        loginAsAdmin();
        navigation.themes();
        page.changeLoginTheme(Theme.KEYCLOAK.getName());
        page.saveTheme();
        logOut();
        page.verifyKeycloakTheme();

        loginAsAdmin();
    }

}