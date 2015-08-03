package org.keycloak.testsuite.console.page;

import org.keycloak.testsuite.console.page.fragment.Navigation;
import static org.keycloak.testsuite.page.auth.AuthRealm.TEST;
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
        return configureMenu;
    }

    public class ConfigureMenu extends Navigation {

        @FindBy(partialLinkText = "Realm Settings")
        private WebElement realmSettingsLink;
        @FindBy(partialLinkText = "Clients")
        private WebElement clientsLink;
        @FindBy(partialLinkText = "Roles")
        private WebElement rolesLink;
        @FindBy(partialLinkText = "Identity Providers")
        private WebElement identityProvidersLink;
        @FindBy(partialLinkText = "User Feferation")
        private WebElement userFederationLink;
        @FindBy(partialLinkText = "Authentication")
        private WebElement authenticationLink;

        public void realmSettings() {
            clickAndWaitForHeader(realmSettingsLink);
        }

        public void clients() {
            clickAndWaitForHeader(clientsLink);
        }

        public void roles() {
            clickAndWaitForHeader(rolesLink);
        }

        public void identityProviders() {
            clickAndWaitForHeader(identityProvidersLink);
        }

        public void userFederation() {
            clickAndWaitForHeader(userFederationLink);
        }

        public void authentication() {
            clickAndWaitForHeader(authenticationLink);
        }

    }

    @FindBy(xpath = "//div[./h2[text()='Manage']]")
    protected ManageMenu manageMenu;

    public ManageMenu manage() {
        return manageMenu;
    }

    public class ManageMenu extends Navigation {

        @FindBy(partialLinkText = "Users")
        private WebElement usersLink;
        @FindBy(partialLinkText = "Sessions")
        private WebElement sessionsLink;
        @FindBy(partialLinkText = "Events")
        private WebElement eventsLink;

        public void users() {
            clickAndWaitForHeader(usersLink);
        }

        public void sessions() {
            clickAndWaitForHeader(sessionsLink);
        }

        public void events() {
            clickAndWaitForHeader(eventsLink);
        }
    }

}
