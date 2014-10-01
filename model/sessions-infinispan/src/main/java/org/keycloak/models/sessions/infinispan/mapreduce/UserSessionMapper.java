package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionMapper implements Mapper<String, SessionEntity, String, Object>, Serializable {

    public UserSessionMapper(String realm) {
        this.realm = realm;
    }

    private enum EmitValue {
        KEY, ENTITY
    }

    private String realm;

    private EmitValue emit = EmitValue.ENTITY;

    private String user;

    private Long expired;

    private Long expiredRefresh;

    public static UserSessionMapper create(String realm) {
        return new UserSessionMapper(realm);
    }

    public UserSessionMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    public UserSessionMapper user(String user) {
        this.user = user;
        return this;
    }

    public UserSessionMapper expired(long expired, long expiredRefresh) {
        this.expired = expired;
        this.expiredRefresh = expiredRefresh;
        return this;
    }

    @Override
    public void map(String key, SessionEntity e, Collector collector) {
        if (!(e instanceof UserSessionEntity)) {
            return;
        }

        UserSessionEntity entity = (UserSessionEntity) e;

        if (!realm.equals(entity.getRealm())) {
            return;
        }

        if (user != null && !entity.getUser().equals(user)) {
            return;
        }

        if (expired != null && expiredRefresh != null && entity.getStarted() > expired && entity.getLastSessionRefresh() > expiredRefresh) {
            return;
        }

        switch (emit) {
            case KEY:
                collector.emit(key, key);
                break;
            case ENTITY:
                collector.emit(key, entity);
                break;
        }
    }

}
