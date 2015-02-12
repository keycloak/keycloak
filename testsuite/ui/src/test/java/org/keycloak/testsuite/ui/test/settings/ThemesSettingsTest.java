package org.keycloak.testsuite.ui.test.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.page.settings.ThemesSettingsPage;


/**
 * Created by fkiss.
 */
public class ThemesSettingsTest extends AbstractKeyCloakTest<ThemesSettingsPage> {

    private String baseTheme="base";
    private String keycloakTheme="keycloak";
    private String patternflyTheme="patternfly";

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeThemeTest() {
        navigation.themes();
    }

    @Test
    public void changeLoginThemeTest() {
        page.changeLoginTheme(baseTheme);
        page.saveTheme();
        logOut();
        page.verifyBaseTheme();

        loginAsAdmin();
        navigation.themes();
        page.changeLoginTheme(patternflyTheme);
        page.saveTheme();
        logOut();
        page.verifyPatternflyTheme();

        loginAsAdmin();
        navigation.themes();
        page.changeLoginTheme(keycloakTheme);
        page.saveTheme();
        logOut();
        page.verifyKeycloakTheme();

        loginAsAdmin();
    }

}