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

import org.eclipse.jetty.server.Request;
import org.keycloak.adapters.jetty.spi.JettyUserSessionManagement;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.SessionIdMapper;

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Jetty9SamlSessionStore extends JettySamlSessionStore {
    public Jetty9SamlSessionStore(Request request, AdapterSessionStore sessionStore, HttpFacade facade, SessionIdMapper idMapper, JettyUserSessionManagement sessionManagement, SamlDeployment deployment) {
        super(request, sessionStore, facade, idMapper, sessionManagement, deployment);
    }

    @Override
    protected String changeSessionId(HttpSession session) {
        Request request = this.request;
        if (!deployment.turnOffChangeSessionIdOnLogin()) return request.changeSessionId();
        else return session.getId();
    }
}
