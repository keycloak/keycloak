package org.keycloak.models.sessions.infinispan.mapreduce;

import java.io.Serializable;
import java.util.Collection;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * Return all clientSessions attached to any from input list of userSessions
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientSessionsOfUserSessionMapper implements Mapper<String, SessionEntity, String, Object>, Serializable {

    private String realm;
    private Collection<String> userSessions;

    private EmitValue emit = EmitValue.ENTITY;

    private enum EmitValue {
        KEY, ENTITY
    }

    public ClientSessionsOfUserSessionMapper(String realm, Collection<String> userSessions) {
        this.realm = realm;
        this.userSessions = userSessions;
    }

    public ClientSessionsOfUserSessionMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    @Override
    public void map(String key, SessionEntity e, Collector<String, Object> collector) {
        if (!realm.equals(e.getRealm())) {
            return;
        }

        if (!(e instanceof ClientSessionEntity)) {
            return;
        }

        ClientSessionEntity entity = (ClientSessionEntity) e;

        if (userSessions.contains(entity.getUserSession())) {
            switch (emit) {
                case KEY:
                    collector.emit(entity.getId(), entity.getId());
                    break;
                case ENTITY:
                    collector.emit(entity.getId(), entity);
                    break;
            }
        }
    }
}
