package org.keycloak.testsuite.console.page.clients;

import org.keycloak.testsuite.console.page.fragment.Breadcrumb;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Client extends Clients {

    public static final String CLIENT_ID = "clientId"; // TODO client.id vs client.clientId

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + CLIENT_ID + "}";
    }

    public final void setClientId(String clientId) {
        setUriParameter(CLIENT_ID, clientId);
    }

    public String getClientId() {
        return (String) getUriParameter(CLIENT_ID);
    }

    @FindBy(xpath = "//ol[@class='breadcrumb']")
    private Breadcrumb breadcrumb;

    public Breadcrumb breadcrumb() {
        return breadcrumb;
    }

    @FindBy(xpath = "//div[@data-ng-controller='ClientTabCtrl']/ul")
    protected ClientTabs clientTabs;

    public ClientTabs tabs() {
        return clientTabs;
    }

    public class ClientTabs {

        @FindBy(linkText = "Settings")
        private WebElement settingsLink;
        @FindBy(linkText = "Roles")
        private WebElement rolesLink;
        @FindBy(linkText = "Mappers")
        private WebElement mappersLink;
        @FindBy(linkText = "Scope")
        private WebElement scopeLink;
        @FindBy(linkText = "Revocation")
        private WebElement revocationLink;
        @FindBy(linkText = "Sessions")
        private WebElement sessionsLink;
        @FindBy(linkText = "Installation")
        private WebElement installationLink;

        public void settings() {
            settingsLink.click();
        }

        public void roles() {
            rolesLink.click();
        }

        public void mappers() {
            mappersLink.click();
        }

        public void scope() {
            scopeLink.click();
        }

        public void revocation() {
            revocationLink.click();
        }

        public void sessions() {
            sessionsLink.click();
        }

        public void installation() {
            installationLink.click();
        }

    }

}
