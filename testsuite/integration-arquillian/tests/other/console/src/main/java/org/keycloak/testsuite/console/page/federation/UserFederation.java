package org.keycloak.testsuite.console.page.federation;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * Created by fkiss.
 */
public class UserFederation extends AdminConsoleRealm {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/user-federation";
    }

    @FindByJQuery("select[ng-model*='selectedProvider']")
    private Select addProviderSelect;

    public void addProvider(String provider) {
        waitUntilElement(By.cssSelector("select[ng-model*='selectedProvider']")).is().present();
        addProviderSelect.selectByVisibleText(provider);
    }

}
