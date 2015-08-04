package org.keycloak.testsuite.console.page.federation;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElement;

/**
 * Created by fkiss.
 */
public class UserFederationPage extends AdminConsole {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/user-federation";
    }

    @FindByJQuery("select[ng-model*='selectedProvider']")
    private Select addProviderSelect;

    public void addProvider(String provider) {
        waitGuiForElement(By.cssSelector("select[ng-model*='selectedProvider']"));
        addProviderSelect.selectByVisibleText(provider);
    }

}
