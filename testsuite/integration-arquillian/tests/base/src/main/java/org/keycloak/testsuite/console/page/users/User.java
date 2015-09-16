package org.keycloak.testsuite.console.page.users;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.testsuite.console.page.fragment.Breadcrumb;
import static org.keycloak.testsuite.console.page.fragment.Breadcrumb.BREADCRUMB_XPATH;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

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
            attributesLink.click();
        }

        public void credentials() {
            credentialsLink.click();
        }

        public void roleMappings() {
            roleMappingsLink.click();
        }

        public void consents() {
            consentsLink.click();
        }

        public void sessions() {
            sessionsLink.click();
        }

    }
    
    public UserResource userResource() {
        return usersResource().get(getId());
    }

}
