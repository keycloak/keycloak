package org.keycloak.testsuite.console.page.users;

import org.keycloak.testsuite.console.page.fragment.Breadcrumb;
import org.keycloak.testsuite.console.page.fragment.Navigation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class User extends Users {

    public static final String USER_ID = "userId";

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + USER_ID + "}";
    }

    public void setUserId(String userId) {
        setUriParameter(USER_ID, userId);
    }

    public String getUserId() {
        return (String) getUriParameter(USER_ID);
    }

    @FindBy(css = "breadcrumb")
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

}
