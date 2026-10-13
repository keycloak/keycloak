package org.keycloak.models.sessions.infinispan.changes;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Performs an entity replacement in Infinispan, using its versions instead of equality.
 *
 * @param <K> The Infinispan key type.
 * @param <T> The Infinispan value type (Keycloak entity)
 */
@ProtoTypeId(Marshalling.REPLACE_FUNCTION)
public class ReplaceFunction<K, T extends SessionEntity> implements BiFunction<K, SessionEntityWrapper<T>, SessionEntityWrapper<T>> {

    private final UUID expectedVersion;
    private final SessionEntityWrapper<T> newValue;

    @ProtoFactory
    public ReplaceFunction(UUID expectedVersion, SessionEntityWrapper<T> newValue) {
        this.expectedVersion = Objects.requireNonNull(expectedVersion);
        this.newValue = Objects.requireNonNull(newValue);
    }

    @Override
    public SessionEntityWrapper<T> apply(K key, SessionEntityWrapper<T> currentValue) {
        assert currentValue != null;
        return expectedVersion.equals(currentValue.getVersion()) ? newValue : currentValue;
    }

    @ProtoField(1)
    UUID getExpectedVersion() {
        return expectedVersion;
    }

    @ProtoField(2)
    SessionEntityWrapper<T> getNewValue() {
        return newValue;
    }
}
