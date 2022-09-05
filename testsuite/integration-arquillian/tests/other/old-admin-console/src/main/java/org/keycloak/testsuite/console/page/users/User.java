package org.keycloak.testsuite.console.page.users;

import org.keycloak.testsuite.console.page.fragment.Breadcrumb;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.console.page.fragment.Breadcrumb.BREADCRUMB_XPATH;
import static org.keycloak.testsuite.util.WaitUtils.*;

/**
 *
 * @author tkyjovsk
 */
public class User extends Users {

    public static final String ID = "id";

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + ID + "}";
    }

    public void setId(String id) {
        setUriParameter(ID, id);
    }

    public String getId() {
        return (String) getUriParameter(ID);
    }

    @FindBy(xpath = BREADCRUMB_XPATH)
    private Breadcrumb breadcrumb;

    public Breadcrumb breadcrumb() {
        return breadcrumb;
    }
    
    @FindBy(xpath = "//div[@data-ng-controller='UserTabCtrl']/ul")
    protected UserTabs userTabs;

    public UserTabs tabs() {
        return userTabs;
    }

    public class UserTabs {

        @FindBy(linkText = "Attributes")
        private WebElement attributesLink;
        @FindBy(linkText = "Credentials")
        private WebElement credentialsLink;
        @FindBy(linkText = "Role Mappings")
        private WebElement roleMappingsLink;
        @FindBy(linkText = "Consents")
        private WebElement consentsLink;
        @FindBy(linkText = "Sessions")
        private WebElement sessionsLink;

        public void attributes() {
            waitUntilElement(attributesLink).is().present();
            attributesLink.click();
        }

        public void credentials() {
            waitUntilElement(consentsLink).is().present();
            credentialsLink.click();
        }

        public void roleMappings() {
            waitUntilElement(roleMappingsLink).is().present();
            roleMappingsLink.click();
        }

        public void consents() {
            waitUntilElement(consentsLink).is().present();
            consentsLink.click();
        }

        public void sessions() {
            waitUntilElement(sessionsLink).is().present();
            sessionsLink.click();
        }

    }
    
}
