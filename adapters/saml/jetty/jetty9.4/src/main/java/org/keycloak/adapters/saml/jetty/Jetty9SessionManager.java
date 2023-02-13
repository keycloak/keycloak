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

import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionHandler;
import org.keycloak.adapters.jetty.spi.JettySessionManager;

import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Jetty9SessionManager implements JettySessionManager {
    protected SessionHandler sessionHandler;

    public Jetty9SessionManager(SessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    @Override
    public HttpSession getHttpSession(String extendedId) {
        // inlined code from sessionHandler.getHttpSession(extendedId) since the method visibility changed to protected

        String id = sessionHandler.getSessionIdManager().getId(extendedId);
        Session session = sessionHandler.getSession(id);

        if (session != null && !session.getExtendedId().equals(extendedId)) {
            session.setIdChanged(true);
        }
        return session;
    }
}
