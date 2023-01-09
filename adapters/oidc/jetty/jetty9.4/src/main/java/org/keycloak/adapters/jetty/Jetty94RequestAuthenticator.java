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

package org.keycloak.adapters.jetty;

import org.eclipse.jetty.server.Request;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.jetty.core.JettyRequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Jetty94RequestAuthenticator extends JettyRequestAuthenticator {
    public Jetty94RequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, AdapterTokenStore tokenStore, int sslRedirectPort, Request request) {
        super(facade, deployment, tokenStore, sslRedirectPort, request);
    }

    @Override
    protected String changeHttpSessionId(boolean create) {
        Request request = this.request;
        HttpSession session = request.getSession(false);
        if (session == null) {
            return request.getSession(true).getId();
        }
        if (!deployment.isTurnOffChangeSessionIdOnLogin()) return request.changeSessionId();
        else return session.getId();
    }
}
