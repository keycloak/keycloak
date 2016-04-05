package org.keycloak.testsuite.console.realm;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.console.page.fragment.Dropdown;
import org.keycloak.testsuite.console.page.realm.ThemeSettings;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import static org.junit.Assert.*;
import static org.keycloak.testsuite.util.WaitUtils.*;
import static org.keycloak.testsuite.util.URLAssert.*;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class InternationalizationTest extends AbstractRealmTest {
    @Page
    private ThemeSettings themeSettingsPage;

    @FindBy(id = "kc-locale-dropdown")
    private Dropdown localeDropdown;

    @Before
    public void beforeInternationalizationTest() {
        realmSettingsPage.navigateTo();
        tabs().themes();
        themeSettingsPage.setInternatEnabled(true);
        themeSettingsPage.saveTheme();
        assertAlertSuccess();
        realmSettingsPage.setAdminRealm(AuthRealm.TEST);
        accountPage.setAuthRealm(testRealmPage);
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

        localeDropdown.selectByText("Español");
        assertLocale(".//label[@for='password']", "Contraseña"); // Password

        loginToTestRealmConsoleAs(testUser);
        assertConsoleLocale("Temas");

        accountPage.navigateTo();
        assertAccountLocale("Cuenta");
    }

    /**
     * Change locale on the Account page
     */
    @Test
    public void accountInternationalization() {
        accountPage.navigateTo();
        loginPage.form().login(testUser);

        localeDropdown.selectByText("Français");
        accountPage.navigateTo();
        assertAccountLocale("Compte");

        deleteAllCookiesForTestRealm();

        loginToTestRealmConsoleAs(testUser);
        assertConsoleLocale("Thèmes");
    }

    private void assertConsoleLocale(String expected) {
        pause(500);
        assertCurrentUrlEquals(realmSettingsPage);
        assertLocale(".//a[contains(@href,'/theme-settings')]", expected); // Themes
    }

    private void assertAccountLocale(String expected) {
        pause(500);
        assertCurrentUrlEquals(accountPage);
        assertLocale(".//div[contains(@class,'bs-sidebar')]/ul/li", expected); // Account
    }

    private void assertLocale(String xpathSelector, String expected) {
        WebElement element = driver.findElement(By.xpath(xpathSelector));
        assertLocale(element, expected);
    }

    private void assertLocale(WebElement element, String expected) {
        waitUntilElement(element);
        assertEquals(expected, element.getText());
    }
}
