/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testframework.ui.page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfoPage extends AbstractPage {

    @FindBy(className = "instruction")
    private WebElement infoMessage;

    @FindBy(linkText = "« Back to Application")
    private WebElement backToApplicationLink;

    @FindBy(linkText = "» Klicken Sie hier um fortzufahren")
    private WebElement clickToContinueDe;

    @FindBy(linkText = "» Click here to proceed")
    private WebElement clickToContinue;

    @FindBy(linkText = "« Zpět na aplikaci")
    private WebElement backToApplicationCs;

    public InfoPage(WebDriver driver) {
        super(driver);
    }

    public String getInfo() {
        return infoMessage.getText();
    }

    public void clickBackToApplicationLink() {
        backToApplicationLink.click();
    }

    public String getBackToApplicationLink() {
        return backToApplicationLink != null
                ? backToApplicationLink.getAttribute("href")
                : null;
    }

    public void clickToContinueDe() {
        clickToContinueDe.click();
    }

    public void clickToContinue() {
        clickToContinue.click();
    }

    public void clickBackToApplicationLinkCs() {
        backToApplicationCs.click();
    }

    @Override
    public String getExpectedPageId() {
        return "info";
    }

    @Override
    public boolean isCurrent() {
        return driver.getPageSource().contains("kc-info-message");
    }
}
