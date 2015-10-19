package org.keycloak.adapters.spi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps external principal and SSO id to internal local http session id
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InMemorySessionIdMapper implements SessionIdMapper {
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
        if (sso != null) {
            ssoToSession.put(sso, session);
            sessionToSso.put(session, sso);
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
