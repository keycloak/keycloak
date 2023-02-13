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

package org.keycloak.testsuite.adapter.page;

import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractShowTokensPage extends AbstractPageWithInjectedUrl {

    @FindBy(id = "accessToken")
    private WebElement accessToken;

    @FindBy(id = "refreshToken")
    private WebElement refreshToken;

    @FindBy(id = "accessTokenString")
    private WebElement accessTokenString;

    @FindBy(id = "refreshTokenString")
    private WebElement refreshTokenString;

    public AccessToken getAccessToken() {
        try {
            return JsonSerialization.readValue(accessToken.getText(), AccessToken.class);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchElementException nsee) {
            log.warn("No accessToken element found on the page");
        }

        return null;
    }


    public RefreshToken getRefreshToken() {
        try {
            return JsonSerialization.readValue(refreshToken.getText(), RefreshToken.class);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchElementException nsee) {
            log.warn("No refreshToken element found on the page");
        }

        return null;
    }


    public String getAccessTokenString() {
        try {
            return accessTokenString.getText();
        } catch (NoSuchElementException nsee) {
            log.warn("No accessTokenString element found on the page");
        }

        return null;
    }

    public String getRefreshTokenString() {
        try {
            return refreshTokenString.getText();
        } catch (NoSuchElementException nsee) {
            log.warn("No refreshTokenString element found on the page");
        }

        return null;
    }
}
