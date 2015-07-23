package org.keycloak.testsuite.page.console;

import java.util.List;
import javax.ws.rs.core.UriBuilder;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class RealmsRoot extends AdminConsole {

    @FindBy(xpath = "//tr[@data-ng-repeat='r in realms']//a[contains(@class,'ng-binding')]")
    private List<WebElement> realmLinks;

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("/");
    }

    @Override
    public String getFragment() {
        return "/realms";
    }

    public void clickRealm(String realm) {
        boolean linkFound = false;
        for (WebElement realmLink : realmLinks) {
            if (realmLink.getText().equals(realm)) {
                linkFound = true;
                realmLink.click();
            }
        }
        if (!linkFound) {
            throw new IllegalStateException("A link for realm '" + realm + "' not found on the Realms page.");
        }
    }

}
