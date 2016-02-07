package org.keycloak.testsuite.console.page.clients;

import org.jboss.arquillian.graphene.fragment.Root;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.testsuite.console.page.fragment.Breadcrumb;
import static org.keycloak.testsuite.console.page.fragment.Breadcrumb.BREADCRUMB_XPATH;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Client extends Clients {

    public static final String ID = "id"; // TODO client.id vs client.clientId

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + ID + "}";
    }

    public final void setId(String id) {
        setUriParameter(ID, id);
    }

    public String getId() {
        return getUriParameter(ID).toString();
    }

    @FindBy(xpath = BREADCRUMB_XPATH)
    private Breadcrumb breadcrumb;

    public Breadcrumb breadcrumb() {
        return breadcrumb;
    }

    public void backToClientsViaBreadcrumb() {
        breadcrumb.clickItemOneLevelUp();
    }

    @FindBy(id = "removeClient")
    private WebElement deleteIcon;
    
    public void delete() {
        deleteIcon.click();
        modalDialog.confirmDeletion();
    }

    @FindBy(xpath = "//div[@data-ng-controller='ClientTabCtrl']/ul")
    protected ClientTabs clientTabs;

    public ClientTabs tabs() {
        return clientTabs;
    }

    public class ClientTabs {

        @Root
        private WebElement tabs;
        
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
        
        public WebElement getTabs() {
            return tabs;
        }

    }



}
