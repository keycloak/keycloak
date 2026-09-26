package org.keycloak.models.sessions.infinispan.changes;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

import org.infinispan.functional.EntryView.ReadWriteEntryView;
import org.infinispan.functional.MetaParam.MetaLifespan;
import org.infinispan.functional.MetaParam.MetaMaxIdle;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/** A failed version comparison leaves both the value and its expiration metadata untouched. */
@ProtoTypeId(Marshalling.SESSION_COMPARE_AND_SET)
public final class SessionCompareAndSet<K, V extends SessionEntity>
        implements Function<ReadWriteEntryView<K, SessionEntityWrapper<V>>, Boolean> {
    private final UUID expectedVersion;
    private final SessionEntityWrapper<V> replacement;
    private final long lifespan;
    private final long maxIdle;

    @ProtoFactory
    public SessionCompareAndSet(UUID expectedVersion, SessionEntityWrapper<V> replacement, long lifespan, long maxIdle) {
        this.expectedVersion = Objects.requireNonNull(expectedVersion);
        this.replacement = replacement;
        this.lifespan = lifespan;
        this.maxIdle = maxIdle;
    }

    @Override
    public Boolean apply(ReadWriteEntryView<K, SessionEntityWrapper<V>> entry) {
        var current = entry.find();
        if (current.isEmpty() || !expectedVersion.equals(current.get().getVersion())) {
            return false;
        }
        if (replacement == null) {
            entry.remove();
        } else {
            entry.set(replacement, new MetaLifespan(lifespan), new MetaMaxIdle(maxIdle));
        }
        return true;
    }

    @ProtoField(1)
    UUID getExpectedVersion() {
        return expectedVersion;
    }

    @ProtoField(2)
    SessionEntityWrapper<V> getReplacement() {
        return replacement;
    }

    @ProtoField(value = 3, defaultValue = "-1")
    long getLifespan() {
        return lifespan;
    }

    @ProtoField(value = 4, defaultValue = "-1")
    long getMaxIdle() {
        return maxIdle;
    }
}
