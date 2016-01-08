package org.keycloak.models.sessions.infinispan.stream;

import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionPredicate implements Predicate<Map.Entry<String, SessionEntity>>, Serializable {

    private String realm;

    private String user;

    private Integer expired;

    private Integer expiredRefresh;

    private String brokerSessionId;
    private String brokerUserId;

    private UserSessionPredicate(String realm) {
        this.realm = realm;
    }

    public static UserSessionPredicate create(String realm) {
        return new UserSessionPredicate(realm);
    }

    public UserSessionPredicate user(String user) {
        this.user = user;
        return this;
    }

    public UserSessionPredicate expired(Integer expired, Integer expiredRefresh) {
        this.expired = expired;
        this.expiredRefresh = expiredRefresh;
        return this;
    }

    public UserSessionPredicate brokerSessionId(String id) {
        this.brokerSessionId = id;
        return this;
    }

    public UserSessionPredicate brokerUserId(String id) {
        this.brokerUserId = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, SessionEntity> entry) {
        SessionEntity e = entry.getValue();

        if (!(e instanceof UserSessionEntity)) {
            return false;
        }

        UserSessionEntity entity = (UserSessionEntity) e;

        if (!realm.equals(entity.getRealm())) {
            return false;
        }

        if (user != null && !entity.getUser().equals(user)) {
            return false;
        }

        if (brokerSessionId != null && !brokerSessionId.equals(entity.getBrokerSessionId())) {
            return false;
        }

        if (brokerUserId != null && !brokerUserId.equals(entity.getBrokerUserId())) {
            return false;
        }

        if (expired != null && expiredRefresh != null && entity.getStarted() > expired && entity.getLastSessionRefresh() > expiredRefresh) {
            return false;
        }

        if (expired == null && expiredRefresh != null && entity.getLastSessionRefresh() > expiredRefresh) {
            return false;
        }

        return true;
    }
}
