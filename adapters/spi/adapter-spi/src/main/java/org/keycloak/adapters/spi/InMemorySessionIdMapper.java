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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Maps external principal and SSO id to internal local http session id
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InMemorySessionIdMapper implements SessionIdMapper {

    private static final Logger LOG = Logger.getLogger(InMemorySessionIdMapper.class.getName());

    ConcurrentHashMap<String, String> ssoToSession = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, String> sessionToSso = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Set<String>> principalToSession = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, String> sessionToPrincipal = new ConcurrentHashMap<>();

    @Override
    public boolean hasSession(String id) {
        return sessionToSso.containsKey(id) || sessionToPrincipal.containsKey(id);
    }

    @Override
    public void clear() {
        ssoToSession.clear();
        sessionToSso.clear();
        principalToSession.clear();
        sessionToPrincipal.clear();
    }

    @Override
    public Set<String> getUserSessions(String principal) {
        Set<String> lookup = principalToSession.get(principal);
        if (lookup == null) return null;
        Set<String> copy = new HashSet<>();
        copy.addAll(lookup);
        return copy;
    }

    @Override
    public String getSessionFromSSO(String sso) {
        return ssoToSession.get(sso);
    }

    @Override
    public void map(String sso, String principal, String session) {
        LOG.debugf("Adding mapping (%s, %s, %s)", sso, principal, session);

        if (sso != null) {
            ssoToSession.put(sso, session);
            sessionToSso.put(session, sso);
        }

        if (principal == null) {
            return;
        }

        Set<String> userSessions = principalToSession.get(principal);
        if (userSessions == null) {
            final Set<String> tmp = Collections.synchronizedSet(new HashSet<String>());
            userSessions = principalToSession.putIfAbsent(principal, tmp);
            if (userSessions == null) {
                userSessions = tmp;
            }
        }
        userSessions.add(session);
        sessionToPrincipal.put(session, principal);
    }

    @Override
    public void removeSession(String session) {
        LOG.debugf("Removing session %s", session);

        String sso = sessionToSso.remove(session);
        if (sso != null) {
            ssoToSession.remove(sso);
        }
        String principal =  sessionToPrincipal.remove(session);
        if (principal != null) {
            Set<String> sessions = principalToSession.get(principal);
            sessions.remove(session);
            if (sessions.isEmpty()) {
                principalToSession.remove(principal, sessions);
            }
        }
    }


}
