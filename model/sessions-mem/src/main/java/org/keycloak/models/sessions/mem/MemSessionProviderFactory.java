package org.keycloak.models.sessions.mem;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.LoginFailure;
import org.keycloak.models.sessions.SessionProvider;
import org.keycloak.models.sessions.SessionProviderFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemSessionProviderFactory implements SessionProviderFactory {

    private ConcurrentHashMap<SessionKey, SessionAdapter> sessions = new ConcurrentHashMap<SessionKey, SessionAdapter>();
    private ConcurrentHashMap<LoginFailureKey, LoginFailureAdapter> loginFailures = new ConcurrentHashMap<LoginFailureKey, LoginFailureAdapter>();

    @Override
    public SessionProvider create(KeycloakSession session) {
        return new MemSessionProvider(sessions, loginFailures);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
        sessions.clear();
        loginFailures.clear();
    }

    @Override
    public String getId() {
        return "mem";
    }

}
