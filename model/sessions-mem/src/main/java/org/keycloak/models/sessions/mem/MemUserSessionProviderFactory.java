package org.keycloak.models.sessions.mem;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.mem.entities.ClientSessionEntity;
import org.keycloak.models.sessions.mem.entities.UserSessionEntity;
import org.keycloak.models.sessions.mem.entities.UsernameLoginFailureEntity;
import org.keycloak.models.sessions.mem.entities.UsernameLoginFailureKey;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MemUserSessionProviderFactory implements UserSessionProviderFactory {

    public static final String ID = "mem";

    private ConcurrentHashMap<String, UserSessionEntity> userSessions = new ConcurrentHashMap<String, UserSessionEntity>();

    private ConcurrentHashMap<String, ClientSessionEntity> clientSessions = new ConcurrentHashMap<String, ClientSessionEntity>();

    private ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity> loginFailures = new ConcurrentHashMap<UsernameLoginFailureKey, UsernameLoginFailureEntity>();
    private final ConcurrentHashMap<String, String> userSessionsByBrokerSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> userSessionsByBrokerUserId = new ConcurrentHashMap<>();

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        return new MemUserSessionProvider(session, userSessions, userSessionsByBrokerSessionId, userSessionsByBrokerUserId, clientSessions, loginFailures);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
        userSessions.clear();
        loginFailures.clear();
        userSessionsByBrokerSessionId.clear();
        userSessionsByBrokerUserId.clear();
    }

    @Override
    public String getId() {
        return ID;
    }

}
