package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserLoginFailureMapper implements Mapper<LoginFailureKey, LoginFailureEntity, LoginFailureKey, Object>, Serializable {

    public UserLoginFailureMapper(String realm) {
        this.realm = realm;
    }

    private enum EmitValue {
        KEY, ENTITY
    }

    private String realm;

    private EmitValue emit = EmitValue.ENTITY;

    public static UserLoginFailureMapper create(String realm) {
        return new UserLoginFailureMapper(realm);
    }

    public UserLoginFailureMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    @Override
    public void map(LoginFailureKey key, LoginFailureEntity e, Collector collector) {
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
