/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.saml.wildfly.infinispan;

import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapperUpdater;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import org.infinispan.Cache;

/**
 *
 * @author hmlnarik
 */
public class SsoCacheSessionIdMapperUpdater implements SessionIdMapperUpdater, SessionListener {

    private final SessionIdMapperUpdater delegate;
    /**
     * Cache where key is a HTTP session ID, and value is a pair (user session ID, principal name) of Strings.
     */
    private final Cache<String, String[]> httpSessionToSsoCache;

    public SsoCacheSessionIdMapperUpdater(Cache<String, String[]> httpSessionToSsoCache, SessionIdMapperUpdater previousIdMapperUpdater) {
        this.delegate = previousIdMapperUpdater;
        this.httpSessionToSsoCache = httpSessionToSsoCache;
    }

    // SessionIdMapperUpdater methods

    @Override
    public void clear(SessionIdMapper idMapper) {
        httpSessionToSsoCache.clear();
        this.delegate.clear(idMapper);
    }

    @Override
    public void map(SessionIdMapper idMapper, String sso, String principal, String httpSessionId) {
        httpSessionToSsoCache.put(httpSessionId, new String[] {sso, principal});
        this.delegate.map(idMapper, sso, principal, httpSessionId);
    }

    @Override
    public void removeSession(SessionIdMapper idMapper, String httpSessionId) {
        httpSessionToSsoCache.remove(httpSessionId);
        this.delegate.removeSession(idMapper, httpSessionId);
    }

    // Undertow HTTP session listener methods

    @Override
    public void sessionCreated(Session session, HttpServerExchange exchange) {
    }

    @Override
    public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
    }

    @Override
    public void attributeAdded(Session session, String name, Object value) {
    }

    @Override
    public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
    }

    @Override
    public void attributeRemoved(Session session, String name, Object oldValue) {
    }

    @Override
    public void sessionIdChanged(Session session, String oldSessionId) {
        this.httpSessionToSsoCache.remove(oldSessionId);
        Object value = session.getAttribute(SamlSession.class.getName());
        if (value instanceof SamlSession) {
            SamlSession sess = (SamlSession) value;
            httpSessionToSsoCache.put(session.getId(), new String[] {sess.getSessionIndex(), sess.getPrincipal().getSamlSubject()});
        }
    }
}
