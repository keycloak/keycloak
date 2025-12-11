/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.pages;

import java.net.URI;
import java.net.URISyntaxException;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.services.Urls;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page represented by code.ftl. It is used by "Installed applications" (KeycloakInstalled)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InstalledAppRedirectPage extends AbstractPage {

    @FindBy(id = "code")
    private WebElement code;

    @FindBy(id = "kc-page-title")
    private WebElement pageTitle;

    @FindBy(css = "div[class^='pf-v5-c-alert'], div[class^='alert-error']")
    private WebElement errorBox;

    public void open(String realmName, String code, String error, String errorDescription) {
        try {
            KeycloakUriBuilder kcUriBuilder = KeycloakUriBuilder.fromUri(Urls.realmInstalledAppUrnCallback(new URI(oauth.AUTH_SERVER_ROOT), realmName));
            if (code != null) {
                kcUriBuilder.queryParam(OAuth2Constants.CODE, code);
            }
            if (error != null) {
                kcUriBuilder.queryParam(OAuth2Constants.ERROR, error);
            }
            if (errorDescription != null) {
                kcUriBuilder.queryParam(OAuth2Constants.ERROR_DESCRIPTION, errorDescription);
            }
            String oobEndpointUri = kcUriBuilder.build().toString();
            driver.navigate().to(oobEndpointUri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException(use);
        }
    }

    @Override
    public boolean isCurrent() {
        throw new UnsupportedOperationException("Use method 'isCurrentExpectSuccess' or 'isCurrentExpectError'");
    }


    public String getSuccessCode() {
        Assert.assertEquals("Success code", getPageTitleText());
        return code.getAttribute("value");
    }

    public String getPageTitleText() {
        return pageTitle.getText();
    }

    // Check if link is present inside title or error box
    public void assertLinkBackToApplicationNotPresent() {
        try {
            pageTitle.findElement(By.tagName("a"));
            throw new AssertionError("Link was present inside title");
        } catch (NoSuchElementException nsee) {
            // Ignore
        }

        try {
            errorBox.findElement(By.tagName("a"));
            throw new AssertionError("Link was present inside error box");
        } catch (NoSuchElementException nsee) {
            // Ignore
        }

    }
}
