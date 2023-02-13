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

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public abstract class Login extends LoginBase {

    public static final String PROTOCOL = "protocol";
    public static final String OIDC = "openid-connect";
    public static final String SAML = "saml";
    public static final String LOGIN_ACTION = "login-action";

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

}
