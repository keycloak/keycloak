package org.keycloak.testsuite.console.page.events;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Events extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment();
    }
    
    @FindBy(linkText = "Login Events")
    private WebElement loginEventsTab;
    @FindBy(linkText = "Admin Events")
    private WebElement adminEventsTab;
    @FindBy(linkText = "Config")
    private WebElement configTab;
    
    public void loginEvents() {
        loginEventsTab.click();
    }
    public void adminEvents() {
        adminEventsTab.click();
    }
    public void config() {
        configTab.click();
    }
    
}
