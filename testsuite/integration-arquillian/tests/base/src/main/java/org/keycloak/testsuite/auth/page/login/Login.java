/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.auth.page.login;

import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.auth.page.AuthRealm;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public abstract class Login extends AuthRealm {

    public static final String PROTOCOL = "protocol";
    public static final String OIDC = "openid-connect";
    public static final String SAML = "saml";


    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .path("protocol/{" + PROTOCOL + "}/auth");
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

    @FindBy(css = "link[href*='login/keycloak/css/login.css']")
    private WebElement keycloakTheme;

    public void waitForKeycloakThemeNotPresent() {
        waitUntilElement(keycloakTheme).is().not().present();
    }

    public void waitForKeycloakThemePresent() {
        waitUntilElement(keycloakTheme).is().present();
    }

}
