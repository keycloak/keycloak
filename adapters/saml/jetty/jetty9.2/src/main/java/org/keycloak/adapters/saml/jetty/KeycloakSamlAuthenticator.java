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

package org.keycloak.adapters.saml.jetty;

import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.keycloak.adapters.jetty.spi.JettyUserSessionManagement;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;

import javax.servlet.ServletRequest;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSamlAuthenticator extends AbstractSamlAuthenticator {

    public KeycloakSamlAuthenticator() {
        super();
    }


   @Override
    protected Request resolveRequest(ServletRequest req) {
        return (req instanceof Request) ? (Request)req : HttpChannel.getCurrentHttpChannel().getRequest();
    }

    @Override
    public Authentication createAuthentication(UserIdentity userIdentity, Request request) {
        return new KeycloakAuthentication(getAuthMethod(), userIdentity) {
            @Override
            public void logout() {
                logoutCurrent(HttpChannel.getCurrentHttpChannel().getRequest());
            }
        };
    }

    @Override
    public AdapterSessionStore createSessionTokenStore(Request request, SamlDeployment resolvedDeployment) {
        return new JettyAdapterSessionStore(request);
    }

    @Override
    public JettyUserSessionManagement createSessionManagement(Request request) {
        return new JettyUserSessionManagement(new Jetty9SessionManager(request.getSessionManager()));
    }

    @Override
    protected JettySamlSessionStore createJettySamlSessionStore(Request request, HttpFacade facade, SamlDeployment resolvedDeployment) {
        JettySamlSessionStore store;
        store = new Jetty9SamlSessionStore(request, createSessionTokenStore(request, resolvedDeployment), facade, idMapper, createSessionManagement(request), resolvedDeployment);
        return store;
    }
}
