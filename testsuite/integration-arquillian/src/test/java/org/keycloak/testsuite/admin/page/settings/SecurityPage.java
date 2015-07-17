/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.admin.page.settings;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.keycloak.testsuite.admin.fragment.OnOffSwitch;
import org.keycloak.testsuite.admin.page.AbstractPage;
import org.keycloak.testsuite.admin.util.SeleniumUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author Filip Kiss
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