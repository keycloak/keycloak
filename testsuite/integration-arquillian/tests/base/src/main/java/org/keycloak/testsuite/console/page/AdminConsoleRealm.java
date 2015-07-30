package org.keycloak.testsuite.console.page;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import static org.keycloak.testsuite.page.auth.AuthRealm.MASTER;
import org.openqa.selenium.WebElement;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsoleRealm extends AdminConsoleRealmsRoot {

    public static final String CONSOLE_REALM = "consoleRealm";
    
    public AdminConsoleRealm() {
        setUriParameter(CONSOLE_REALM, MASTER);
    }

    public AdminConsoleRealm setConsoleRealm(String realm) {
        setUriParameter(CONSOLE_REALM, realm);
        return this;
    }

    public String getConsoleRealm() {
        return getUriParameter(CONSOLE_REALM).toString();
    }

    @Override
    public String getFragment() {
        return super.getFragment() + "/{" + CONSOLE_REALM + "}";
    }

    @FindByJQuery("a:contains('Users')")
    private WebElement usersLink;

    public void clickUsers() {
        usersLink.click();
    }

}
