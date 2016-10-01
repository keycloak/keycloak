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

package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.servlet.spec.ServletContextImpl;

import java.lang.reflect.Method;
import java.security.AccessController;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ChangeSessionId {
    /**
     * This is a hack to be backward compatible between Undertow 1.3+ and versions lower.  In Undertow 1.3, a new
     * switch was added setChangeSessionIdOnLogin, this screws up session management for keycloak as after the session id
     * is uploaded to Keycloak, undertow changes the session id and it can't be invalidated.
     *
     * @param deploymentInfo
     */
    public static void turnOffChangeSessionIdOnLogin(DeploymentInfo deploymentInfo) {
        try {
            Method method = DeploymentInfo.class.getMethod("setChangeSessionIdOnLogin", boolean.class);
            method.invoke(deploymentInfo, false);
        } catch (Exception ignore) {

        }
    }

    public static String changeSessionId(HttpServerExchange exchange, boolean create) {
        final ServletRequestContext sc = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        ServletContextImpl currentServletContext = sc.getCurrentServletContext();
        HttpSessionImpl session = currentServletContext.getSession(exchange, create);
        if (session == null) {
            return null;
        }
        Session underlyingSession;
        if(System.getSecurityManager() == null) {
            underlyingSession = session.getSession();
        } else {
            underlyingSession = AccessController.doPrivileged(new HttpSessionImpl.UnwrapSessionAction(session));
        }


        return underlyingSession.changeSessionId(exchange, currentServletContext.getSessionConfig());
    }
}
