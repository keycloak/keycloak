package org.keycloak.testsuite.ui.page.settings.federation;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.ui.util.SeleniumUtils.waitGuiForElement;

/**
 * Created by fkiss.
 */
public class UserFederationPage extends AbstractPage {


    @FindByJQuery("select[ng-model*='selectedProvider']")
    private Select addProviderSelect;

    public void addProvider(String provider){
        waitGuiForElement(By.cssSelector("select[ng-model*='selectedProvider']"));
        addProviderSelect.selectByVisibleText(provider);
    }

}