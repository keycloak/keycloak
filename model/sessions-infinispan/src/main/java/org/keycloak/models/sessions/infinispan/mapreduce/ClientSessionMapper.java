package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionMapper implements Mapper<String, ClientSessionEntity, String, Object>, Serializable {

    private enum EmitValue {
        KEY, ENTITY, USER_SESSION_AND_TIMESTAMP
    }

    private EmitValue emit = EmitValue.ENTITY;

    private String client;

    private String userSession;

    public static ClientSessionMapper create() {
        return new ClientSessionMapper();
    }

    public ClientSessionMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    public ClientSessionMapper emitUserSessionAndTimestamp() {
        emit = EmitValue.USER_SESSION_AND_TIMESTAMP;
        return this;
    }

    public ClientSessionMapper client(String client) {
        this.client = client;
        return this;
    }

    public ClientSessionMapper userSession(String userSession) {
        this.userSession = userSession;
        return this;
    }

    @Override
    public void map(String key, ClientSessionEntity entity, Collector collector) {
        if (client != null && !entity.getClient().equals(client)) {
            return;
        }

        if (userSession != null && !entity.getUserSession().equals(userSession)) {
            return;
        }

        switch (emit) {
            case KEY:
                collector.emit(key, key);
                break;
            case ENTITY:
                collector.emit(key, entity);
                break;
            case USER_SESSION_AND_TIMESTAMP:
                collector.emit(entity.getUserSession(), entity.getTimestamp());
                break;
        }
    }

}
