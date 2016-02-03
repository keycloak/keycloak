/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.sessions.infinispan.mapreduce;

import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import java.io.Serializable;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionMapper implements Mapper<String, SessionEntity, String, Object>, Serializable {

    public ClientSessionMapper(String realm) {
        this.realm = realm;
    }

    private enum EmitValue {
        KEY, ENTITY, USER_SESSION_AND_TIMESTAMP
    }

    private String realm;

    private EmitValue emit = EmitValue.ENTITY;

    private String client;

    private String userSession;

    private Long expiredRefresh;

    private Boolean requireNullUserSession = false;

    public static ClientSessionMapper create(String realm) {
        return new ClientSessionMapper(realm);
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

    public ClientSessionMapper expiredRefresh(long expiredRefresh) {
        this.expiredRefresh = expiredRefresh;
        return this;
    }

    public ClientSessionMapper requireNullUserSession(boolean requireNullUserSession) {
        this.requireNullUserSession = requireNullUserSession;
        return this;
    }

    @Override
    public void map(String key, SessionEntity e, Collector collector) {
        if (!realm.equals(e.getRealm())) {
            return;
        }

        if (!(e instanceof ClientSessionEntity)) {
            return;
        }

        ClientSessionEntity entity = (ClientSessionEntity) e;

        if (client != null && !entity.getClient().equals(client)) {
            return;
        }

        if (userSession != null && !userSession.equals(entity.getUserSession())) {
            return;
        }

        if (requireNullUserSession && entity.getUserSession() != null) {
            return;
        }

        if (expiredRefresh != null && entity.getTimestamp() > expiredRefresh) {
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
                if (entity.getUserSession() != null) {
                    collector.emit(entity.getUserSession(), entity.getTimestamp());
                }
                break;
        }
    }

}
