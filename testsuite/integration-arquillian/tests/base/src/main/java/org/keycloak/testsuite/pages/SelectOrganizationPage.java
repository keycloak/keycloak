/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

public class SelectOrganizationPage extends LanguageComboboxAwarePage {

    @ArquillianResource
    protected OAuthClient oauth;

    @FindBy(xpath = "//html")
    protected WebElement htmlRoot;

    @Override
    public boolean isCurrent() {
        try {
            return !driver.findElements(By.id("kc-user-organizations")).isEmpty();
        } catch (NoSuchElementException ignore) {}

        return false;
    }

    public void assertCurrent(String realm) {
        String name = getClass().getSimpleName();
        Assert.assertTrue("Expected " + name + " but was " + DroneUtils.getCurrentDriver().getTitle() + " (" + DroneUtils.getCurrentDriver().getCurrentUrl() + ")",
                isCurrent(realm));
    }

    public void selectOrganization(String alias) {
        WebElement socialButton = findOrganizationButton(alias);
        clickLink(socialButton);
    }

    public boolean isOrganizationButtonPresent(String alias) {
        String id = "organization-" + alias;
        return !DroneUtils.getCurrentDriver().findElements(By.id(id)).isEmpty();
    }

    private WebElement findOrganizationButton(String alias) {
        String id = "organization-" + alias;
        return DroneUtils.getCurrentDriver().findElement(By.id(id));
    }
}
