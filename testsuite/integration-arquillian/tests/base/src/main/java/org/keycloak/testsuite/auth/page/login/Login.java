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
package org.keycloak.testsuite.auth.page.login;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public abstract class Login extends AuthRealm {

    public static final String PROTOCOL = "protocol";
    public static final String OIDC = "openid-connect";
    public static final String SAML = "saml";
    public static final String LOGIN_ACTION = "login-action";
    private String keycloakThemeCssName;

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path(((getProtocol().equals(OIDC) || getProtocol().equals(SAML)) ? "protocol/" : "") + "{" + PROTOCOL + "}" + (getProtocol().equals(OIDC) ? "/auth" : ""));
    }
    
    public void setProtocol(String protocol) {
        setUriParameter(PROTOCOL, protocol);
    }
    
    public String getProtocol() {
        return getUriParameter(PROTOCOL).toString();
    }
    
    @Page
    private LoginForm form;

    public LoginForm form() {
        return form;
    }

    public void setKeycloakThemeCssName(String name) {
        keycloakThemeCssName = name;
    }

    protected By getKeycloakThemeLocator() {
        if (keycloakThemeCssName == null) {
            throw new IllegalStateException("keycloakThemeCssName property must be set");
        }
        return By.cssSelector("link[href*='login/" + keycloakThemeCssName + "/css/login.css']");
    }

    public void waitForKeycloakThemeNotPresent() {
        waitUntilElement(getKeycloakThemeLocator()).is().not().present();
    }

    public void waitForKeycloakThemePresent() {
        waitUntilElement(getKeycloakThemeLocator()).is().present();
    }

}
