package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionPredicate implements Predicate<Map.Entry<String, SessionEntity>>, Serializable {

    private String realm;

    private SessionPredicate(String realm) {
        this.realm = realm;
    }

    public static SessionPredicate create(String realm) {
        return new SessionPredicate(realm);
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntity> entry) {
        return realm.equals(entry.getValue().getRealm());
    }

}
