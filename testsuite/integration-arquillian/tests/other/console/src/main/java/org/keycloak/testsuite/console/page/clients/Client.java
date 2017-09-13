package org.keycloak.testsuite.console.page.clients;

import org.jboss.arquillian.graphene.fragment.Root;
import org.keycloak.testsuite.console.page.fragment.Breadcrumb;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.console.page.fragment.Breadcrumb.BREADCRUMB_XPATH;
import static org.keycloak.testsuite.util.UIUtils.clickLink;

import org.openqa.selenium.NoSuchElementException;

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
        clickLink(deleteIcon);
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
        @FindBy(linkText = "Service Account Roles")
        private WebElement serviceAccountRoles;
        @FindBy(linkText = "Authorization")
        private WebElement authorizationLink;

        public void settings() {
            clickLink(settingsLink);
        }

        public void roles() {
            clickLink(rolesLink);
        }

        public void mappers() {
            clickLink(mappersLink);
        }

        public void scope() {
            clickLink(scopeLink);
        }

        public void revocation() {
            clickLink(revocationLink);
        }

        public void sessions() {
            clickLink(sessionsLink);
        }

        public void installation() {
            clickLink(installationLink);
        }

        public void authorization() {
            clickLink(authorizationLink);
        }
        
        public boolean isServiceAccountRolesDisplayed() {
            try {
                return serviceAccountRoles.isDisplayed();
            } catch (NoSuchElementException ex) {
            }
            return false;
        }
        
        public WebElement getTabs() {
            return tabs;
        }

    }



}
