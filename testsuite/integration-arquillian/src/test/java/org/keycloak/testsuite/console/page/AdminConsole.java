package org.keycloak.testsuite.console.page;

import javax.ws.rs.core.UriBuilder;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsole extends AdminRoot {

    @FindBy(xpath = "//ul[contains(@class,'navbar-utility')]//a[contains(@class,'dropdown-toggle')]")
    private WebElement utilityNavbarDropdown;

    @FindBy(linkText = "Sign Out")
    private WebElement signOutLink;

    public AdminConsole() {
        setTemplateValue("consoleRealm", Realm.MASTER);
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("{consoleRealm}/console");
    }

    public void logOut() {
        System.out.println("Logging out.");
        utilityNavbarDropdown.click();
        signOutLink.click();
    }

}
