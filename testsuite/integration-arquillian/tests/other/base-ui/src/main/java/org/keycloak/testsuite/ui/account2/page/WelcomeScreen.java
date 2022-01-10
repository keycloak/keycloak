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

package org.keycloak.testsuite.ui.account2.page;

import org.keycloak.testsuite.ui.account2.page.fragment.WelcomeScreenHeader;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

import static org.keycloak.testsuite.util.UIAssert.assertElementVisible;
import static org.keycloak.testsuite.util.UIUtils.clickLink;
import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class WelcomeScreen extends AbstractAccountPage {
    public static final String ROOT_ELEMENT_ID = "welcomeScreen";

    @FindBy(id = ROOT_ELEMENT_ID)
    private WebElement welcomeScreenRoot;

    @FindBy(xpath = "//*[@id='" + ROOT_ELEMENT_ID + "']//header")
    private WelcomeScreenHeader header;

    @FindBy(xpath = "//a[@id='landing-personal-info']")
    private WebElement personalInfoLink;
    @FindBy(xpath = "//*[@id='landingChangePasswordLink']/a")
    private WebElement changePasswordLink;
    @FindBy(xpath = "//a[@id='landing-authenticator']")
    private WebElement authenticatorLink;
    @FindBy(xpath = "//*[@id='landing-device-activity']/a")
    private WebElement deviceActivityLink;
    @FindBy(xpath = "//*[@id='landing-linked-accounts']/a")
    private WebElement linkedAccountsLink;
    @FindBy(xpath = "//a[@id='landing-applications']")
    private WebElement applicationsLink;
    @FindBy(id = "landing-resources")
    private WebElement myResourcesCard;
    @FindBy(xpath = "//a[@id='landing-resources']")
    private WebElement myResourcesLink;
    @FindBy(id = "landingLogo")
    private WebElement logoLink;

    @FindBy(id = "landingWelcomeMessage")
    private WebElement welcomeMessage; // used only for i18n testing

    private String referrer;
    private String referrerUri;

    @Override
    public boolean isCurrent() {
        return URLUtils.currentUrlEquals(toString() + "#/") && isElementVisible(welcomeScreenRoot); // the hash will be eventually added after the page is loaded
    }

    @Override
    public UriBuilder getUriBuilder() {
        UriBuilder uriBuilder = super.getUriBuilder();
        if (referrer != null && referrerUri != null) {
            uriBuilder.queryParam("referrer", referrer);
            uriBuilder.queryParam("referrer_uri", referrerUri);
        }
        return uriBuilder;
    }

    public WelcomeScreenHeader header() {
        return header;
    }

    public void clickLogoImage() {
        clickLink(logoLink);
    }

    public void clickPersonalInfoLink() {
        clickLink(personalInfoLink);
    }

    public void clickChangePasswordLink() {
        clickLink(changePasswordLink);
    }

    public void clickAuthenticatorLink() {
        clickLink(authenticatorLink);
    }

    public void clickDeviceActivityLink() {
        clickLink(deviceActivityLink);
    }

    public void assertLinkedAccountsLinkVisible(boolean expected) {
        assertElementVisible(expected, linkedAccountsLink);
    }

    public void clickLinkedAccountsLink() {
        clickLink(linkedAccountsLink);
    }

    public void clickApplicationsLink() {
        clickLink(applicationsLink);
    }

    public void assertMyResourcesCardVisible(boolean expected) {
        assertElementVisible(expected, myResourcesCard);
    }

    public void clickMyResourcesLink() {
        clickLink(myResourcesLink);
    }

    public String getWelcomeMessage() {
        return getTextFromElement(welcomeMessage);
    }

    public void navigateTo(String referrer, String referrerUri) {
        this.referrer = referrer;
        this.referrerUri = referrerUri;
        navigateTo();
        this.referrer = null;
        this.referrerUri = null;
    }
}
