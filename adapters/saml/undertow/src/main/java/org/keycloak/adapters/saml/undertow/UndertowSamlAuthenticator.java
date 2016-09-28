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

package org.keycloak.adapters.saml.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.webbrowsersso.BrowserHandler;
import org.keycloak.adapters.spi.HttpFacade;

import java.security.Principal;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowSamlAuthenticator extends SamlAuthenticator {
    protected SecurityContext securityContext;

    public UndertowSamlAuthenticator(SecurityContext securityContext, HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        super(facade, deployment, sessionStore);
        this.securityContext = securityContext;
    }

    @Override
    protected void completeAuthentication(final SamlSession samlSession) {
        Account undertowAccount = new Account() {
            @Override
            public Principal getPrincipal() {
                return samlSession.getPrincipal();
            }

            @Override
            public Set<String> getRoles() {
                return samlSession.getRoles();
            }
        };
        securityContext.authenticationComplete(undertowAccount, "KEYCLOAK-SAML", false);

    }

    @Override
    protected SamlAuthenticationHandler createBrowserHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        return new BrowserHandler(facade, deployment, sessionStore);
    }

}
