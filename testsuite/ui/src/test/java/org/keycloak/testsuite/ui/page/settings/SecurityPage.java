package org.keycloak.testsuite.ui.page.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.ui.fragment.OnOffSwitch;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.keycloak.testsuite.ui.util.SeleniumUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Created by fkiss.
 */
public class SecurityPage extends AbstractPage {

    @FindByJQuery("a:contains('Brute Force Detection')")
    private WebElement bruteForceProtectionLink;

    @FindByJQuery("div[class='onoffswitch']")
    private OnOffSwitch protectionEnabled;

    @FindBy(id = "failureFactor")
    private WebElement failureFactorInput;

    @FindBy(id = "waitIncrement")
    private WebElement waitIncrementInput;

    @FindBy(id = "waitIncrementUnit")
    private Select waitIncrementSelect;

    @FindBy(id = "quickLoginCheckMilliSeconds")
    private WebElement quickLoginCheckInput;

    @FindBy(id = "minimumQuickLoginWait")
    private WebElement minQuickLoginWaitInput;

    @FindBy(id = "minimumQuickLoginWaitUnit")
    private Select minQuickLoginWaitSelect;

    @FindBy(id = "maxFailureWait")
    private WebElement maxWaitInput;

    @FindBy(id = "maxFailureWaitUnit")
    private Select maxWaitSelect;

    @FindBy(id = "maxDeltaTime")
    private WebElement failureResetTimeInput;

    @FindBy(id = "maxDeltaTimeUnit")
    private Select failureResetTimeSelect;

    public void goToAndEnableBruteForceProtectionTab() {
        SeleniumUtils.waitGuiForElement(bruteForceProtectionLink);
        bruteForceProtectionLink.click();
        if(!protectionEnabled.isEnabled()){
            protectionEnabled.enable();
        }
    }

    public void setFailureFactorInput(String value){
        failureFactorInput.clear();
        failureFactorInput.sendKeys(value);
    }

    public void setWaitIncrementInput(String value){
        waitIncrementInput.clear();
        waitIncrementInput.sendKeys(value);
    }

    public void setQuickLoginCheckInput(String value){
        quickLoginCheckInput.clear();
        quickLoginCheckInput.sendKeys(value);
    }

    public void setMinQuickLoginWaitInput(String value){
        minQuickLoginWaitInput.clear();
        minQuickLoginWaitInput.sendKeys(value);
    }

    public void setMaxWaitInput(String value){
        maxWaitInput.clear();
        maxWaitInput.sendKeys(value);
    }

    public void setFailureResetTimeInput(String value){
        failureResetTimeInput.clear();
        failureResetTimeInput.sendKeys(value);
    }

}