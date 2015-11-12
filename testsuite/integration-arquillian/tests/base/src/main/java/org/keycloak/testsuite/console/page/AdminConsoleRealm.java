package org.keycloak.testsuite.console.page;

import org.keycloak.admin.client.resource.RealmResource;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import org.keycloak.testsuite.util.WaitUtils;
import static org.keycloak.testsuite.util.WaitUtils.waitGuiForElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsoleRealm extends AdminConsoleRealmsRoot {

    public static final String CONSOLE_REALM = "consoleRealm";

    public AdminConsoleRealm() {
        setUriParameter(CONSOLE_REALM, TEST);
    }

    public AdminConsoleRealm setConsoleRealm(String realm) {
        setUriParameter(CONSOLE_REALM, realm);
        return this;
    }

    public String getConsoleRealm() {
        return getUriParameter(CONSOLE_REALM).toString();
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + CONSOLE_REALM + "}";
    }

    @FindBy(xpath = "//div[./h2[text()='Configure']]")
    private ConfigureMenu configureMenu;

    public ConfigureMenu configure() {
        waitGuiForElement(By.xpath("//div[./h2[text()='Configure']]"));
        return configureMenu;
    }

    public RealmResource realmResource() {
        return realmsResource().realm(getConsoleRealm());
    }

    public class ConfigureMenu {

        @FindBy(partialLinkText = "Realm Settings")
        private WebElement realmSettingsLink;
        @FindBy(partialLinkText = "Clients")
        private WebElement clientsLink;
        @FindBy(partialLinkText = "Roles")
        private WebElement rolesLink;
        @FindBy(partialLinkText = "Identity Providers")
        private WebElement identityProvidersLink;
        @FindBy(partialLinkText = "User Federation")
        private WebElement userFederationLink;
        @FindBy(partialLinkText = "Authentication")
        private WebElement authenticationLink;

        public void realmSettings() {
            realmSettingsLink.click();
        }

        public void clients() {
            clientsLink.click();
        }

        public void roles() {
            rolesLink.click();
        }

        public void identityProviders() {
            identityProvidersLink.click();
        }

        public void userFederation() {
            userFederationLink.click();
        }

        public void authentication() {
            authenticationLink.click();
        }

    }

    @FindBy(xpath = "//div[./h2[text()='Manage']]")
    protected ManageMenu manageMenu;

    public ManageMenu manage() {
        WaitUtils.waitGuiForElement(By.xpath("//div[./h2[text()='Manage']]"));
        return manageMenu;
    }

    public class ManageMenu {

        @FindBy(partialLinkText = "Users")
        private WebElement usersLink;
        @FindBy(partialLinkText = "Sessions")
        private WebElement sessionsLink;
        @FindBy(partialLinkText = "Events")
        private WebElement eventsLink;

        public void users() {
            usersLink.click();
        }

        public void sessions() {
            sessionsLink.click();
        }

        public void events() {
            eventsLink.click();
        }
    }

}
