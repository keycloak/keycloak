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

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.SESSION_ENTITY_WRAPPER)
public class SessionEntityWrapper<S extends SessionEntity> {

    private final UUID version;
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

    @ProtoField(1)
    public UUID getVersion() {
        return version;
    }

    public S getEntity() {
        return entity;
    }

    @ProtoField(2)
    WrappedMessage getEntityPS() {
        return new WrappedMessage(getEntity());
    }

    @ProtoField(value = 3, mapImplementation = ConcurrentHashMap.class)
    public Map<String, String> getLocalMetadata() {
        return localMetadata;
    }

    @ProtoFactory
    static <T extends SessionEntity> SessionEntityWrapper<T> create(UUID version, WrappedMessage entityPS, Map<String, String> localMetadata) {
        assert entityPS != null;
        T entity = (T) entityPS.getValue();
        if (version == null) {
            return new SessionEntityWrapper<>(entity);
        }
        return new SessionEntityWrapper<>(version, localMetadata, entity);
    }

    public ClientModel getClientIfNeeded(RealmModel realm) {
        if (entity instanceof AuthenticatedClientSessionEntity) {
            String clientId = ((AuthenticatedClientSessionEntity) entity).getClientId();
            if (clientId != null) {
                return realm.getClientById(clientId);
            }
        }
        return null;
    }

    public String getLocalMetadataNote(String key) {
        if (isForTransport()) {
            throw new IllegalStateException("This entity is only intended for transport");
        }
        return localMetadata.get(key);
    }

    public Integer getLocalMetadataNoteInt(String key) {
        String note = getLocalMetadataNote(key);
        return note==null ? null : Integer.valueOf(note);
    }

    public void putLocalMetadataNoteInt(String key, int value) {
        if (isForTransport()) {
            throw new IllegalStateException("This entity is only intended for transport");
        }
        localMetadata.put(key, String.valueOf(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SessionEntityWrapper)) {
            return false;
        }

        SessionEntityWrapper<?> that = (SessionEntityWrapper<?>) o;

        return Objects.equals(version, that.version) && Objects.equals(entity, that.entity);
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

}
