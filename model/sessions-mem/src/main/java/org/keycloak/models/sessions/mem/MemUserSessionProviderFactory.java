package org.keycloak.models.sessions.mem;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.mem.entities.UserSessionEntity;
import org.keycloak.models.sessions.mem.entities.UserSessionKey;
import org.keycloak.models.sessions.mem.entities.UsernameLoginFailureEntity;
import org.keycloak.models.sessions.mem.entities.UsernameLoginFailureKey;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemUserSessionProviderFactory implements UserSessionProviderFactory {

    private ConcurrentHashMap<UserSessionKey, UserSessionEntity> sessions = new ConcurrentHashMap<UserSessionKey, UserSessionEntity>();

    private ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures = new ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity>();

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        return new MemUserSessionProvider(session, sessions, loginFailures);
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
