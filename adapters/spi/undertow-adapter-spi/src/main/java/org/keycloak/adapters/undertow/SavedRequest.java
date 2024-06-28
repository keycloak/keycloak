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
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.security.AccessController;

/**
 * Saved servlet request.
 *
 * Note bill burke: I had to fork this because Undertow was automatically restoring the request before the code could be
 * processed and redirected.
 *
 * CachedAuthenticatedSessionHandler was restoring the request before the authentication manager could read the code from the URI
 * Originally, I copied SavedRequest as is, but there are type mismatches between Undertow 1.1.1 and 1.3.10.
 * So, trySaveRequest calls the same undertow version, removes the saved request, stores it in a different session attribute,
 * then restores the old attribute later
 *
 *
 * @author Stuart Douglas
 */
public class SavedRequest implements Serializable {

    private static final String SESSION_KEY = SavedRequest.class.getName();

    public static void trySaveRequest(final HttpServerExchange exchange) {
        io.undertow.servlet.util.SavedRequest.trySaveRequest(exchange);
        final ServletRequestContext sc = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpSessionImpl session = sc.getCurrentServletContext().getSession(exchange, true);
        Session underlyingSession;
        if(System.getSecurityManager() == null) {
            underlyingSession = session.getSession();
        } else {
            underlyingSession = AccessController.doPrivileged(new HttpSessionImpl.UnwrapSessionAction(session));
        }
        io.undertow.servlet.util.SavedRequest request = (io.undertow.servlet.util.SavedRequest) underlyingSession.removeAttribute(io.undertow.servlet.util.SavedRequest.class.getName());
        if (request != null) underlyingSession.setAttribute(SESSION_KEY, request);


    }

    public static void tryRestoreRequest(final HttpServerExchange exchange, HttpSession session) {
        if(session instanceof HttpSessionImpl) {

            Session underlyingSession;
            if(System.getSecurityManager() == null) {
                underlyingSession = ((HttpSessionImpl) session).getSession();
            } else {
                underlyingSession = AccessController.doPrivileged(new HttpSessionImpl.UnwrapSessionAction(session));
            }
            io.undertow.servlet.util.SavedRequest request = (io.undertow.servlet.util.SavedRequest) underlyingSession.removeAttribute(SESSION_KEY);
            if (request != null) {
                underlyingSession.setAttribute(io.undertow.servlet.util.SavedRequest.class.getName(), request);
                io.undertow.servlet.util.SavedRequest.tryRestoreRequest(exchange, session);

            }

         }
    }

}
