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
package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.SessionIdMapper;

import java.util.Objects;
import javax.servlet.http.*;

/**
 *
 * @author hmlnarik
 */
public class IdMapperUpdaterSessionListener implements HttpSessionListener, HttpSessionAttributeListener {

    private final SessionIdMapper idMapper;

    public IdMapperUpdaterSessionListener(SessionIdMapper idMapper) {
        this.idMapper = idMapper;
    }

    @Override
    public void sessionCreated(HttpSessionEvent hse) {
        HttpSession session = hse.getSession();
        Object value = session.getAttribute(SamlSession.class.getName());
        map(session.getId(), value);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent hse) {
        HttpSession session = hse.getSession();
        unmap(session.getId(), session.getAttribute(SamlSession.class.getName()));
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent hsbe) {
        HttpSession session = hsbe.getSession();
        if (Objects.equals(hsbe.getName(), SamlSession.class.getName())) {
            map(session.getId(), hsbe.getValue());
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent hsbe) {
        HttpSession session = hsbe.getSession();
        if (Objects.equals(hsbe.getName(), SamlSession.class.getName())) {
            unmap(session.getId(), hsbe.getValue());
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent hsbe) {
        HttpSession session = hsbe.getSession();
        if (Objects.equals(hsbe.getName(), SamlSession.class.getName())) {
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
