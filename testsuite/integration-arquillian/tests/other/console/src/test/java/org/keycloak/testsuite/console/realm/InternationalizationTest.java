package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.console.page.fragment.LocaleDropdown;
import org.keycloak.testsuite.console.page.realm.ThemeSettings;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.URLAssert.*;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class InternationalizationTest extends AbstractRealmTest {
    private static final String THEME_NAME = "internat-test";
    private static final String LOCALE_CS_NAME = "Čeština";

    private static final String LABEL_CS_PASSWORD = "Heslo";
    private static final String LABEL_CS_REALM_SETTINGS = "Nastavení Realmu";
    private static final String LABEL_CS_EDIT_ACCOUNT = "Upravit účet";

    @Page
    private ThemeSettings themeSettingsPage;

    @FindBy(id = "kc-locale-dropdown")
    private LocaleDropdown localeDropdown;

    @Before
    public void beforeInternationalizationTest() {
        RealmRepresentation realmRepresentation = testRealmResource().toRepresentation();
        realmRepresentation.setAccountTheme(THEME_NAME);
        realmRepresentation.setAdminTheme(THEME_NAME);
        realmRepresentation.setEmailTheme(THEME_NAME);
        realmRepresentation.setLoginTheme(THEME_NAME);
        testRealmResource().update(realmRepresentation);

        realmSettingsPage.navigateTo();
        tabs().themes();
        themeSettingsPage.setInternatEnabled(true);
        themeSettingsPage.saveTheme();
        assertAlertSuccess();
        realmSettingsPage.setAdminRealm(AuthRealm.TEST);
        deleteAllCookiesForTestRealm();
        deleteAllCookiesForMasterRealm();
    }

    @After
    public void afterInternationalizationTest() {
        testContext.setAdminLoggedIn(false);
    }

    /**
     * Change locale before login
     */
    @Test
    public void loginInternationalization() {
        testRealmAdminConsolePage.navigateTo();

        localeDropdown.selectByText(LOCALE_CS_NAME);
        assertLocale(".//label[@for='password']", LABEL_CS_PASSWORD);

        loginPage.form().login(testUser);
        assertConsoleLocale(LABEL_CS_REALM_SETTINGS);

        testRealmAccountPage.navigateTo();
        assertAccountLocale(LABEL_CS_EDIT_ACCOUNT);
    }

    /**
     * Change locale on the Account page
     */
    @Test
    public void accountInternationalization() {
        testRealmAccountPage.navigateTo();
        loginPage.form().login(testUser);

        localeDropdown.selectByText(LOCALE_CS_NAME);
        testRealmAccountPage.navigateTo();
        assertAccountLocale(LABEL_CS_EDIT_ACCOUNT);

        deleteAllCookiesForTestRealm();

        loginToTestRealmConsoleAs(testUser);
        assertConsoleLocale(LABEL_CS_REALM_SETTINGS);
    }

    private void assertConsoleLocale(String expected) {
        assertCurrentUrlEquals(realmSettingsPage);
        assertLocale(".//div[@class='nav-category'][1]/ul/li[1]//a", expected); // Realm Settings
    }

    private void assertAccountLocale(String expected) {
        assertCurrentUrlEquals(testRealmAccountPage);
        assertLocale(".//div[contains(@class,'content-area')]/div[@class='row']/div/h2", expected); // Edit Account
    }

    private void assertLocale(String xpathSelector, String expected) {
        WebElement element = driver.findElement(By.xpath(xpathSelector));
        assertLocale(element, expected);
    }

    private void assertLocale(WebElement element, String expected) {
        assertEquals(expected, getTextFromElement(element));
    }
}
