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
package org.keycloak.adapters.saml.elytron;

import java.util.Objects;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

import org.keycloak.adapters.saml.SamlSession;
import org.keycloak.adapters.spi.SessionIdMapper;

import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class IdMapperUpdaterSessionListener implements HttpSessionListener, HttpSessionAttributeListener, HttpSessionIdListener {

    private static final Logger LOG = Logger.getLogger(IdMapperUpdaterSessionListener.class);

    private final SessionIdMapper idMapper;

    public IdMapperUpdaterSessionListener(SessionIdMapper idMapper) {
        this.idMapper = idMapper;
    }

    @Override
    public void sessionCreated(HttpSessionEvent hse) {
        LOG.debugf("Session created");
        HttpSession session = hse.getSession();
        Object value = session.getAttribute(SamlSession.class.getName());
        map(session.getId(), value);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent hse) {
        LOG.debugf("Session destroyed");
        HttpSession session = hse.getSession();
        unmap(session.getId(), session.getAttribute(SamlSession.class.getName()));
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent hse, String oldSessionId) {
        LOG.debugf("Session changed ID from %s", oldSessionId);
        HttpSession session = hse.getSession();
        Object value = session.getAttribute(SamlSession.class.getName());
        unmap(oldSessionId, value);
        map(session.getId(), value);
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent hsbe) {
        HttpSession session = hsbe.getSession();
        if (Objects.equals(hsbe.getName(), SamlSession.class.getName())) {
            LOG.debugf("Attribute added");
            map(session.getId(), hsbe.getValue());
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent hsbe) {
        HttpSession session = hsbe.getSession();
        if (Objects.equals(hsbe.getName(), SamlSession.class.getName())) {
            LOG.debugf("Attribute removed");
            unmap(session.getId(), hsbe.getValue());
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent hsbe) {
        HttpSession session = hsbe.getSession();
        if (Objects.equals(hsbe.getName(), SamlSession.class.getName())) {
            LOG.debugf("Attribute replaced");
            unmap(session.getId(), hsbe.getValue());
            map(session.getId(), session.getAttribute(SamlSession.class.getName()));
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
