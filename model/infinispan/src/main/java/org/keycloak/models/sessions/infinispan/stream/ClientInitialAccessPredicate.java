package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.entities.ClientInitialAccessEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessPredicate implements Predicate<Map.Entry<String, SessionEntity>>, Serializable {

    public ClientInitialAccessPredicate(String realm) {
        this.realm = realm;
    }

    private String realm;

    private Integer expired;

    public static ClientInitialAccessPredicate create(String realm) {
        return new ClientInitialAccessPredicate(realm);
    }

    public ClientInitialAccessPredicate expired(int time) {
        this.expired = time;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntity> entry) {
        SessionEntity e = entry.getValue();

        if (!realm.equals(e.getRealm())) {
            return false;
        }

        if (!(e instanceof ClientInitialAccessEntity)) {
            return false;
        }

        ClientInitialAccessEntity entity = (ClientInitialAccessEntity) e;

        if (expired != null) {
            if (entity.getRemainingCount() <= 0) {
                return true;
            } else if (entity.getExpiration() > 0 && (entity.getTimestamp() + entity.getExpiration()) < expired) {
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

}
