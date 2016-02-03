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
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionNoteMapper implements Mapper<String, SessionEntity, String, Object>, Serializable {

    public UserSessionNoteMapper(String realm) {
        this.realm = realm;
    }

    private enum EmitValue {
        KEY, ENTITY
    }

    private String realm;

    private EmitValue emit = EmitValue.ENTITY;
    private Map<String, String> notes;

    public static UserSessionNoteMapper create(String realm) {
        return new UserSessionNoteMapper(realm);
    }

    public UserSessionNoteMapper emitKey() {
        emit = EmitValue.KEY;
        return this;
    }

    public UserSessionNoteMapper notes(Map<String, String> notes) {
        this.notes = notes;
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

        for (Map.Entry<String, String> entry : notes.entrySet()) {
            String note = entity.getNotes().get(entry.getKey());
            if (note == null) return;
            if (!note.equals(entry.getValue())) return;
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
