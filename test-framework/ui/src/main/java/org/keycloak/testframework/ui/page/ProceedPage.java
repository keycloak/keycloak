/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
 *
 * @author hmlnarik
 */
public class ProceedPage extends AbstractPage {

    @FindBy(className = "instruction")
    private WebElement infoMessage;

    @FindBy(linkText = "» Click here to proceed")
    private WebElement proceedLink;

    public ProceedPage(WebDriver driver) {
        super(driver);
    }

    public String getInfo() {
        return infoMessage.getText();
    }

    public void clickProceedLink() {
        proceedLink.click();
    }

    @Override
    public String getExpectedPageId() {
        return "login-info";
    }
}
