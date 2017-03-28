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
package org.keycloak.adapters.saml.undertow;

import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.spi.SessionIdMapper;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import java.util.Objects;

/**
 *
 * @author hmlnarik
 */
public class IdMapperUpdaterSessionListener implements SessionListener {

    private final SessionIdMapper idMapper;

    public IdMapperUpdaterSessionListener(SessionIdMapper idMapper) {
        this.idMapper = idMapper;
    }

    @Override
    public void sessionCreated(Session session, HttpServerExchange exchange) {
        Object value = session.getAttribute(SamlSession.class.getName());
        map(session.getId(), value);
    }

    @Override
    public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
        if (reason != SessionDestroyedReason.UNDEPLOY) {
            unmap(session.getId(), session.getAttribute(SamlSession.class.getName()));
        }
    }

    @Override
    public void attributeAdded(Session session, String name, Object value) {
        if (Objects.equals(name, SamlSession.class.getName())) {
            map(session.getId(), value);
        }
    }

    @Override
    public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
        if (Objects.equals(name, SamlSession.class.getName())) {
            unmap(session.getId(), oldValue);
            map(session.getId(), newValue);
        }
    }

    @Override
    public void attributeRemoved(Session session, String name, Object oldValue) {
        if (Objects.equals(name, SamlSession.class.getName())) {
            unmap(session.getId(), oldValue);
        }
    }

    @Override
    public void sessionIdChanged(Session session, String oldSessionId) {
        Object value = session.getAttribute(SamlSession.class.getName());
        if (value != null) {
            unmap(oldSessionId, value);
            map(session.getId(), value);
        }
    }

    private void map(String sessionId, Object value) {
        if (! (value instanceof SamlSession) || sessionId == null) {
            return;
        }
        SamlSession account = (SamlSession) value;

        idMapper.map(account.getSessionIndex(), account.getPrincipal().getSamlSubject(), sessionId);
    }

    private void unmap(String sessionId, Object value) {
        if (! (value instanceof SamlSession) || sessionId == null) {
            return;
        }

        SamlSession samlSession = (SamlSession) value;
        if (samlSession.getSessionIndex() != null) {
            idMapper.removeSession(sessionId);
        }
    }

}
