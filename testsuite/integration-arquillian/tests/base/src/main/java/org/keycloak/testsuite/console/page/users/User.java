package org.keycloak.testsuite.console.page.users;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.testsuite.console.page.fragment.Breadcrumb;
import static org.keycloak.testsuite.console.page.fragment.Breadcrumb.BREADCRUMB_XPATH;
import org.keycloak.testsuite.console.page.fragment.Navigation;
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

    public class UserTabs extends Navigation {

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
            clickAndWaitForHeader(attributesLink);
        }

        public void credentials() {
            clickAndWaitForHeader(credentialsLink);
        }

        public void roleMappings() {
            clickAndWaitForHeader(roleMappingsLink);
        }

        public void consents() {
            clickAndWaitForHeader(consentsLink);
        }

        public void sessions() {
            clickAndWaitForHeader(sessionsLink);
        }

    }
    
    public UserResource userResource() {
        return usersResource().get(getId());
    }

}
