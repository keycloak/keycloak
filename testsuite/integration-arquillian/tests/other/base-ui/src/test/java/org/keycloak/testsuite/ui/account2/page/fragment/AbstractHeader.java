/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2.page.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractHeader extends AbstractFragmentWithMobileLayout {
    public static int MOBILE_WIDTH = 991;

    @Root
    private WebElement headerRoot;

    @Override
    protected int getMobileWidth() {
        return MOBILE_WIDTH;
    }

    public void clickLogoutBtn() {
        clickToolsBtn(getLogoutBtn());
    }

    public void assertLogoutBtnVisible(boolean expected) {
        assertToolsBtnVisible(expected, getLogoutBtn());
    }

    public void clickReferrerLink() {
        clickToolsBtn(getReferrerLink());
    }

    public void assertReferrerLinkVisible(boolean expected) {
        assertToolsBtnVisible(expected, getReferrerLink());
    }

    public String getReferrerLinkText() {
        return getToolsBtnText(getReferrerLink());
    }

    public void selectLocale(String locale) {
        if (isMobileLayout()) {
            clickMobileKebab();
        }
        getLocaleBtn().click();
        clickLink(getLocaleDropdown().findElement(By.id(getLocaleElementIdPrefix() + locale)));
    }

    public String getCurrentLocaleName() {
        if (!isMobileLayout()) {
            return getTextFromElement(getLocaleBtn());
        }
        else {
            clickMobileKebab();
            String ret = getTextFromElement(getLocaleBtn());
            clickMobileKebab(); // hide the dropdown again
            return ret;
        }
    }

    public void assertLocaleVisible(boolean expected) {
        assertToolsBtnVisible(expected, getLocaleBtn());
    }

    public abstract void clickMobileKebab();

    protected abstract WebElement getLocaleBtn();

    protected abstract WebElement getLocaleDropdown();

    protected abstract WebElement getLogoutBtn ();

    protected abstract WebElement getReferrerLink();

    protected void clickToolsBtn(WebElement btn) {
        if (!isMobileLayout()) {
            clickLink(btn);
        }
        else {
            clickMobileKebab();
            clickLink(btn);
        }
    }

    protected boolean isToolsBtnVisible(WebElement btn) {
        if (!isMobileLayout()) {
            return isElementVisible(btn);
        }
        else {
            clickMobileKebab();
            boolean ret = isElementVisible(btn);
            clickMobileKebab(); // hide the dropdown again
            return ret;
        }
    }

    protected void assertToolsBtnVisible(boolean expected, WebElement btn) {
        boolean actual = isToolsBtnVisible(btn);
        String msg = "Header button should" + (!expected ? " not" : "") + " be visible";
        assertEquals(msg, expected, actual);
    }

    protected String getToolsBtnText(WebElement btn) {
        if (!isMobileLayout()) {
            return getTextFromElement(btn);
        }
        else {
            clickMobileKebab();
            String ret = getTextFromElement(btn);
            clickMobileKebab(); // hide the dropdown again
            return ret;
        }
    }

    protected String getLocaleElementIdPrefix() {
        return (isMobileLayout() ? "mobile-" : "") + "locale-";
    }
}
