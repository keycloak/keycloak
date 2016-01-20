package org.keycloak.adapters.springsecurity.management;

import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by scott on 4/24/15.
 */
public class LocalSessionManagementStrategy implements SessionManagementStrategy {

    private final Map<String, HttpSession> sessions = new ConcurrentHashMap<String, HttpSession>();

    @Override
    public void clear() {
        sessions.clear();
    }

    @Override
    public Collection<HttpSession> getAll() {
        return sessions.values();
    }

    @Override
    public void store(HttpSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public HttpSession remove(String id) {
        return sessions.remove(id);
    }
}
