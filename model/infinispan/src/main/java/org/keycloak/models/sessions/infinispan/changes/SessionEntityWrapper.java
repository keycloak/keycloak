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

package org.keycloak.models.sessions.infinispan.changes;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.sessions.infinispan.changes.sessions.SessionData;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(SessionEntityWrapper.ExternalizerImpl.class)
public class SessionEntityWrapper<S extends SessionEntity> {

    private UUID version;
    private final S entity;
    private final Map<String, String> localMetadata;


    protected SessionEntityWrapper(UUID version, Map<String, String> localMetadata, S entity) {
        if (version == null) {
            throw new IllegalArgumentException("Version UUID can't be null");
        }

        this.version = version;
        this.localMetadata = localMetadata;
        this.entity = entity;
    }

    public SessionEntityWrapper(Map<String, String> localMetadata, S entity) {
        this(UUID.randomUUID(),localMetadata, entity);
    }

    public SessionEntityWrapper(S entity) {
        this(new ConcurrentHashMap<>(), entity);
    }


    public UUID getVersion() {
        return version;
    }

    public void setVersion(UUID version) {
        this.version = version;
    }


    public S getEntity() {
        return entity;
    }

    public String getLocalMetadataNote(String key) {
        return localMetadata.get(key);
    }

    public void putLocalMetadataNote(String key, String value) {
        localMetadata.put(key, value);
    }

    public Integer getLocalMetadataNoteInt(String key) {
        String note = getLocalMetadataNote(key);
        return note==null ? null : Integer.parseInt(note);
    }

    public void putLocalMetadataNoteInt(String key, int value) {
        localMetadata.put(key, String.valueOf(value));
    }

    public Map<String, String> getLocalMetadata() {
        return localMetadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionEntityWrapper)) return false;

        SessionEntityWrapper that = (SessionEntityWrapper) o;

        if (!Objects.equals(version, that.version)) {
            return false;
        }

        return Objects.equals(entity, that.entity);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(version) * 17
                + Objects.hashCode(entity);
    }

    @Override
    public String toString() {
        return "SessionEntityWrapper{" + "version=" + version + ", entity=" + entity + ", localMetadata=" + localMetadata + '}';
    }

    public static class ExternalizerImpl implements Externalizer<SessionEntityWrapper> {


        @Override
        public void writeObject(ObjectOutput output, SessionEntityWrapper obj) throws IOException {
            MarshallUtil.marshallUUID(obj.version, output, false);
            MarshallUtil.marshallMap(obj.localMetadata, output);
            output.writeObject(obj.getEntity());
        }


        @Override
        public SessionEntityWrapper readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            UUID objVersion = MarshallUtil.unmarshallUUID(input, false);

            Map<String, String> localMetadata = MarshallUtil.unmarshallMap(input, new MarshallUtil.MapBuilder<String, String, Map<String, String>>() {

                @Override
                public Map<String, String> build(int size) {
                    return new ConcurrentHashMap<>(size);
                }

            });

            SessionEntity entity = (SessionEntity) input.readObject();

            return new SessionEntityWrapper<>(objVersion, localMetadata, entity);
        }

    }
}
