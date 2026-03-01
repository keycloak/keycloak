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
package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.UIUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorPage extends LanguageComboboxAwarePage {

    @ArquillianResource
    protected OAuthClient oauth;

    @FindBy(className = "instruction")
    private WebElement errorMessage;

    @FindBy(id = "backToApplication")
    private WebElement backToApplicationLink;

    public String getError() {
        return UIUtils.getTextFromElement(errorMessage);
    }

    public void clickBackToApplication() {
        backToApplicationLink.click();
    }

    public String getBackToApplicationLink() {
        if (backToApplicationLink == null) {
            return null;
        } else {
            return backToApplicationLink.getAttribute("href");
        }
    }

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver) != null && PageUtils.getPageTitle(driver).equals("We are sorry...");
    }

}
