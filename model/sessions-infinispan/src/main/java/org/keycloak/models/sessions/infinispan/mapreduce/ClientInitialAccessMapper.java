package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.keycloak.models.sessions.infinispan.entities.ClientInitialAccessEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessMapper implements Mapper<String, SessionEntity, String, Object>, Serializable {

    public ClientInitialAccessMapper(String realm) {
        this.realm = realm;
    }

    private enum EmitValue {
        KEY, ENTITY
    }

    private String realm;

    private EmitValue emit = EmitValue.ENTITY;

    private Integer time;
    private Integer remainingCount;

    public static ClientInitialAccessMapper create(String realm) {
        return new ClientInitialAccessMapper(realm);
    }

    public ClientInitialAccessMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    public ClientInitialAccessMapper time(int time) {
        this.time = time;
        return this;
    }


    public ClientInitialAccessMapper remainingCount(int remainingCount) {
        this.remainingCount = remainingCount;
        return this;
    }

    @Override
    public void map(String key, SessionEntity e, Collector collector) {
        if (!realm.equals(e.getRealm())) {
            return;
        }

        if (!(e instanceof ClientInitialAccessEntity)) {
            return;
        }

        ClientInitialAccessEntity entity = (ClientInitialAccessEntity) e;

        if (time != null && entity.getExpiration() > 0 && (entity.getTimestamp() + entity.getExpiration()) < time) {
            return;
        }

        if (remainingCount != null && entity.getRemainingCount() == remainingCount) {
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
