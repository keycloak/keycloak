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

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthGrantPage extends LanguageComboboxAwarePage {

    // Locale-resolved built-in client scope consents
    public static final String PROFILE_CONSENT_TEXT = "User profile";
    public static final String EMAIL_CONSENT_TEXT = "Email address";
    public static final String ADDRESS_CONSENT_TEXT = "Address";
    public static final String PHONE_CONSENT_TEXT = "Phone number";
    public static final String OFFLINE_ACCESS_CONSENT_TEXT = "Offline Access";

    @FindBy(css = "input[name=\"accept\"]")
    private WebElement acceptButton;
    @FindBy(css = "input[name=\"cancel\"]")
    private WebElement cancelButton;


    public void accept(){
        acceptButton.click();
    }

    public void cancel(){
        cancelButton.click();
    }

    @Override
    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).contains("Grant Access to ");
    }

    @Override
    public void open() {
    }

    public List<String> getDisplayedGrants() {
        List<String> table = new LinkedList<>();
        WebElement divKcOauth = driver.findElement(By.id("kc-oauth"));
        for (WebElement li : divKcOauth.findElements(By.tagName("li"))) {
            WebElement span = li.findElement(By.tagName("span"));
            table.add(span.getText());
        }
        return table;
    }


    public void assertGrants(String... grants) {
        List<String> displayed = getDisplayedGrants();
        Assert.assertEquals(displayed.size(), grants.length);
        for (String grant : grants) {
            Assert.assertTrue("Requested grant " + grant + " not present. Displayed grants: " + displayed, displayed.contains(grant));
        }
    }

}
