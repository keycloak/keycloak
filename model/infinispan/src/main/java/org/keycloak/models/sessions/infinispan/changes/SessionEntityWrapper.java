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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import java.util.HashMap;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(SessionEntityWrapper.ExternalizerImpl.class)
public class SessionEntityWrapper<S extends SessionEntity> {

    private static final Logger log = Logger.getLogger(SessionEntityWrapper.class);

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
        this(UUID.randomUUID(), localMetadata, entity);
    }

    public SessionEntityWrapper(S entity) {
        this(new ConcurrentHashMap<>(), entity);
    }

    private SessionEntityWrapper(S entity, boolean forTransport) {
        if (! forTransport) {
            throw new IllegalArgumentException("This constructor is only for transport entities");
        }

        this.version = null;
        this.localMetadata = null;
        this.entity = entity;
    }

    public static <S extends SessionEntity> SessionEntityWrapper<S> forTransport(S entity) {
        return new SessionEntityWrapper<>(entity, true);
    }

    public SessionEntityWrapper<S> forTransport() {
        return new SessionEntityWrapper<>(this.entity, true);
    }

    private boolean isForTransport() {
        return this.version == null;
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
        if (isForTransport()) {
            throw new IllegalStateException("This entity is only intended for transport");
        }
        return localMetadata.get(key);
    }

    public void putLocalMetadataNote(String key, String value) {
        if (isForTransport()) {
            throw new IllegalStateException("This entity is only intended for transport");
        }
        localMetadata.put(key, value);
    }

    public Integer getLocalMetadataNoteInt(String key) {
        String note = getLocalMetadataNote(key);
        return note==null ? null : Integer.parseInt(note);
    }

    public void putLocalMetadataNoteInt(String key, int value) {
        if (isForTransport()) {
            throw new IllegalStateException("This entity is only intended for transport");
        }
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

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, SessionEntityWrapper obj) throws IOException {
            output.writeByte(VERSION_1);

            final boolean forTransport = obj.isForTransport();
            output.writeBoolean(forTransport);

            if (! forTransport) {
                output.writeLong(obj.getVersion().getMostSignificantBits());
                output.writeLong(obj.getVersion().getLeastSignificantBits());
                MarshallUtil.marshallMap(obj.localMetadata, output);
            }

            output.writeObject(obj.entity);
        }


        @Override
        public SessionEntityWrapper readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            byte version = input.readByte();

            if (version != VERSION_1) {
                throw new IOException("Invalid version: " + version);
            }
            final boolean forTransport = input.readBoolean();

            if (forTransport) {
                final SessionEntity entity = (SessionEntity) input.readObject();
                final SessionEntityWrapper res = new SessionEntityWrapper(entity);
                if (log.isTraceEnabled()) {
                    log.tracef("Loaded entity from remote store: %s, version=%s, metadata=%s", entity, res.version, res.localMetadata);
                }
                return res;
            } else {
                UUID sessionVersion = new UUID(input.readLong(), input.readLong());
                HashMap<String, String> map = MarshallUtil.unmarshallMap(input, HashMap::new);
                final SessionEntity entity = (SessionEntity) input.readObject();
                if (log.isTraceEnabled()) {
                    log.tracef("Found entity locally: entity=%s, version=%s, metadata=%s", entity, sessionVersion, map);
                }
                return new SessionEntityWrapper(sessionVersion, map, entity);
            }
        }

    }
}
