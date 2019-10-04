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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.clickLink;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class WelcomeScreenHeader extends AbstractHeader {
    @FindBy(id = "landingSignOutButton")
    private WebElement logoutBtn;
    @FindBy(id = "landingSignOutLink")
    private WebElement logoutBtnMobile;

    @FindBy(id = "landingSignInButton")
    private WebElement loginBtn;
    @FindBy(id = "landingSignInLink")
    private WebElement loginBtnMobile;

    @FindBy(xpath = "//*[@id='landing-locale-dropdown']/button")
    private WebElement localeBtn;
    @FindBy(id = "landing-mobile-local-toggle")
    private WebElement localeBtnMobile;

    @FindBy(id = "landing-locale-dropdown-list")
    private WebElement localeDropdown;
    @FindBy(id = "landingMobileDropdown") // the mobile locale menu is integrated with the generic mobile menu
    private WebElement localeDropdownMobile;

    @FindBy(id = "landingReferrerLink")
    private WebElement referrerLink;
    @FindBy(id = "landingMobileReferrerLink")
    private WebElement referrerLinkMobile;

    @FindBy(id = "landingMobileDropdown")
    private WebElement mobileKebab;

    @Override
    public void clickMobileKebab() {
        clickLink(mobileKebab);
    }

    public void clickLoginBtn() {
        clickToolsBtn(isMobileLayout() ? loginBtnMobile : loginBtn);
    }

    public void assertLoginBtnVisible(boolean expected) {
        assertToolsBtnVisible(expected, isMobileLayout() ? loginBtnMobile : loginBtn);
    }

    @Override
    protected WebElement getLocaleBtn() {
        return isMobileLayout() ? localeBtnMobile : localeBtn;
    }

    @Override
    protected WebElement getLocaleDropdown() {
        return isMobileLayout() ? localeDropdownMobile : localeDropdown;
    }

    @Override
    protected WebElement getLogoutBtn() {
        return isMobileLayout() ? logoutBtnMobile : logoutBtn;
    }

    @Override
    protected WebElement getReferrerLink() {
        return isMobileLayout() ? referrerLinkMobile : referrerLink;
    }

    @Override
    protected String getLocaleElementIdPrefix() {
        return "landing-" + super.getLocaleElementIdPrefix();
    }
}
