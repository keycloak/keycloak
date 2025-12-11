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

package org.keycloak.testsuite.webauthn.pages.fragments;

import org.openqa.selenium.WebElement;

import static org.keycloak.testsuite.util.UIUtils.click;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

import static org.junit.Assert.assertEquals;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractHeader extends AbstractFragmentWithMobileLayout {
    public static int MOBILE_WIDTH = 991;

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

    public abstract void clickOptions();

    protected abstract WebElement getLogoutBtn ();

    protected void clickToolsBtn(WebElement btn) {
        clickOptions();
        click(btn);
    }

    protected boolean isToolsBtnVisible(WebElement btn) {
        clickOptions();
        boolean ret = isElementVisible(btn);
        clickOptions(); // hide the dropdown again
        return ret;
    }

    protected void assertToolsBtnVisible(boolean expected, WebElement btn) {
        boolean actual = isToolsBtnVisible(btn);
        String msg = "Header button should" + (!expected ? " not" : "") + " be visible";
        assertEquals(msg, expected, actual);
    }

    protected String getToolsBtnText(WebElement btn) {
        clickOptions();
        String ret = getTextFromElement(btn);
        clickOptions(); // hide the dropdown again
        return ret;
    }

    protected String getLocaleElementIdPrefix() {
        return (isMobileLayout() ? "mobile-" : "") + "locale-";
    }
}
