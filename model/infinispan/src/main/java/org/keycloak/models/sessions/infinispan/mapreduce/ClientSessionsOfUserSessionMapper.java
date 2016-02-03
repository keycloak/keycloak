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
