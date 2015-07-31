package org.keycloak.testsuite.console.page.sessions;

import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Sessions extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/sessions";
    }

    @FindBy(linkText = "Realm Sessions")
    private WebElement realmSessionsTab;

    @FindBy(linkText = "Revocation")
    private WebElement revocationTab;

    public void realmSessions() {
        realmSessionsTab.click();
    }

    public void revocation() {
        revocationTab.click();
    }

}
