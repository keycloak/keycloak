package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SessionMapper implements Mapper<String, SessionEntity, String, Object>, Serializable {

    public SessionMapper(String realm) {
        this.realm = realm;
    }

    private enum EmitValue {
        KEY, ENTITY
    }

    private String realm;

    private EmitValue emit = EmitValue.ENTITY;

    public static SessionMapper create(String realm) {
        return new SessionMapper(realm);
    }

    public SessionMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    @Override
    public void map(String key, SessionEntity e, Collector collector) {
        if (!realm.equals(e.getRealm())) {
            return;
        }

        switch (emit) {
            case KEY:
                collector.emit(key, key);
                break;
            case ENTITY:
                collector.emit(key, e);
                break;
        }
    }

}
