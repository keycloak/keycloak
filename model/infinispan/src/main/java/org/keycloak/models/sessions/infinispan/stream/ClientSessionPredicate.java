package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionPredicate implements Predicate<Map.Entry<String, SessionEntity>>, Serializable {

    private String realm;

    private String client;

    private String userSession;

    private Long expiredRefresh;

    private Boolean requireUserSession = false;

    private Boolean requireNullUserSession = false;

    private ClientSessionPredicate(String realm) {
        this.realm = realm;
    }

    public static ClientSessionPredicate create(String realm) {
        return new ClientSessionPredicate(realm);
    }

    public ClientSessionPredicate client(String client) {
        this.client = client;
        return this;
    }

    public ClientSessionPredicate userSession(String userSession) {
        this.userSession = userSession;
        return this;
    }

    public ClientSessionPredicate expiredRefresh(long expiredRefresh) {
        this.expiredRefresh = expiredRefresh;
        return this;
    }

    public ClientSessionPredicate requireUserSession() {
        requireUserSession = true;
        return this;
    }

    public ClientSessionPredicate requireNullUserSession() {
        requireNullUserSession = true;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntity> entry) {
        SessionEntity e = entry.getValue();

        if (!realm.equals(e.getRealm())) {
            return false;
        }

        if (!(e instanceof ClientSessionEntity)) {
            return false;
        }

        ClientSessionEntity entity = (ClientSessionEntity) e;

        if (client != null && !entity.getClient().equals(client)) {
            return false;
        }

        if (userSession != null && !userSession.equals(entity.getUserSession())) {
            return false;
        }

        if (requireUserSession && entity.getUserSession() == null) {
            return false;
        }

        if (requireNullUserSession && entity.getUserSession() != null) {
            return false;
        }

        if (expiredRefresh != null && entity.getTimestamp() > expiredRefresh) {
            return false;
        }

        return true;
    }

}
