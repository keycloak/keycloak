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

package org.keycloak.adapters.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jboss.logging.Logger;

/**
 * Maps external principal and SSO id to internal local http session id
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InMemorySessionIdMapper implements SessionIdMapper {

    private static final Logger LOG = Logger.getLogger(InMemorySessionIdMapper.class.getName());

    private final Map<String, Set<String>> ssoToSessions = new HashMap<>();
    private final Map<String, String> sessionToSso = new HashMap<>();
    private final Map<String, Set<String>> principalToSessions = new HashMap<>();
    private final Map<String, String> sessionToPrincipal = new HashMap<>();

    @Override
    public synchronized boolean hasSession(String id) {
        return sessionToSso.containsKey(id);
    }

    @Override
    public synchronized void clear() {
        ssoToSessions.clear();
        sessionToSso.clear();
        principalToSessions.clear();
        sessionToPrincipal.clear();
    }

    @Override
    public synchronized Set<String> getUserSessions(String principal) {
        Set<String> sessions = principalToSessions.get(principal);
        return sessions == null ? null : new HashSet<>(sessions);
    }

    @Override
    public synchronized Set<String> getSessionsFromSSO(String sso) {
        Set<String> sessions = ssoToSessions.get(sso);
        return sessions == null ? Set.of() : Set.copyOf(sessions);
    }

    @Override
    public synchronized void map(String sso, String principal, String session) {
        LOG.debugf("Adding mapping (%s, %s, %s)", sso, principal, session);

        Objects.requireNonNull(session, "session");
        unmap(session);
        // The local session is still authenticated when the assertion has no SessionIndex or NameID.
        sessionToSso.put(session, sso);
        if (sso != null) {
            ssoToSessions.computeIfAbsent(sso, key -> new HashSet<>()).add(session);
        }
        if (principal != null) {
            principalToSessions.computeIfAbsent(principal, key -> new HashSet<>()).add(session);
            sessionToPrincipal.put(session, principal);
        }
    }

    @Override
    public synchronized void removeSession(String session) {
        LOG.debugf("Removing session %s", session);
        unmap(session);
    }

    private void unmap(String session) {
        removeMapping(ssoToSessions, sessionToSso.remove(session), session);
        removeMapping(principalToSessions, sessionToPrincipal.remove(session), session);
    }

    private static void removeMapping(Map<String, Set<String>> index, String key, String session) {
        if (key != null) {
            Set<String> sessions = index.get(key);
            sessions.remove(session);
            if (sessions.isEmpty()) {
                index.remove(key);
            }
        }
    }
}
