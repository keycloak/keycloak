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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

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
    public static final String ROLES_CONSENT_TEXT = "User roles";

    @FindBy(css = "[name=\"accept\"]")
    private WebElement acceptButton;
    @FindBy(css = "[name=\"cancel\"]")
    private WebElement cancelButton;


    public void accept(){
        clickLink(acceptButton);
    }

    public void cancel(){
        clickLink(cancelButton);
    }

    @Override
    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).contains("Grant Access to ");
    }

    public List<String> getDisplayedGrants() {
        List<String> table = new ArrayList<>();
        WebElement divKcOauth = driver.findElement(By.id("kc-oauth"));
        for (WebElement li : divKcOauth.findElements(By.tagName("li"))) {
            WebElement span = li.findElement(By.tagName("span"));
            table.add(span.getText());
        }
        return table;
    }


    public void assertGrants(String... expectedGrants) {
        List<String> displayed = getDisplayedGrants();
        List<String> expected = Arrays.asList(expectedGrants);
        Assert.assertTrue("Not matched grants. Displayed grants: " + displayed + ", expected grants: " + expected,
                displayed.containsAll(expected) && expected.containsAll(displayed));
    }

}
