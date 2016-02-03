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


import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.testsuite.OAuthClient;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AppPage extends AbstractPage {

    public static final String AUTH_SERVER_URL = "http://localhost:8081/auth";
    public static final String baseUrl = "http://localhost:8081/app";

    @FindBy(id = "account")
    private WebElement accountLink;

    @Override
    public void open() {
        driver.navigate().to(baseUrl);
    }

    @Override
    public boolean isCurrent() {
        return driver.getCurrentUrl().startsWith(baseUrl);
    }

    public RequestType getRequestType() {
        return RequestType.valueOf(driver.getTitle());
    }

    public void openAccount() {
        accountLink.click();
    }

    public enum RequestType {
        AUTH_RESPONSE, LOGOUT_REQUEST, APP_REQUEST
    }

    public void logout() {
        String logoutUri = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(AUTH_SERVER_URL))
                .queryParam(OAuth2Constants.REDIRECT_URI,baseUrl).build("test").toString();
        driver.navigate().to(logoutUri);

    }

}
