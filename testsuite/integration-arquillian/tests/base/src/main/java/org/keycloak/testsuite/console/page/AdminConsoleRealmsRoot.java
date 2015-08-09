package org.keycloak.testsuite.console.page;

import java.util.List;
import javax.ws.rs.core.UriBuilder;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.testsuite.console.page.fragment.RealmSelector;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class AdminConsoleRealmsRoot extends AdminConsole {

    @FindBy(xpath = "//tr[@data-ng-repeat='r in realms']//a[contains(@class,'ng-binding')]")
    private List<WebElement> realmLinks;

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("/");
    }

    @Override
    public String getUriFragment() {
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

    @FindBy(css = "realm-selector")
    protected RealmSelector realmSelector;

    public RealmsResource realmsResource() {
        return keycloak.realms();
    }

}
