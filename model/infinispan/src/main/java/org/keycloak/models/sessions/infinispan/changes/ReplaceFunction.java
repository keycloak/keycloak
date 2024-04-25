package org.keycloak.models.sessions.infinispan.changes;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * Performs an entity replacement in Infinispan, using its versions instead of equality.
 *
 * @param <K> The Infinispan key type.
 * @param <T> The Infinispan value type (Keycloak entity)
 */
public class ReplaceFunction<K, T extends SessionEntity> implements BiFunction<K, SessionEntityWrapper<T>, SessionEntityWrapper<T>> {

    @SuppressWarnings({"removal", "rawtypes"})
    public static final AdvancedExternalizer<ReplaceFunction> INSTANCE = new Externalizer();
    private final UUID expectedVersion;
    private final SessionEntityWrapper<T> newValue;

    public ReplaceFunction(UUID expectedVersion, SessionEntityWrapper<T> newValue) {
        this.expectedVersion = Objects.requireNonNull(expectedVersion);
        this.newValue = Objects.requireNonNull(newValue);
    }

    @Override
    public SessionEntityWrapper<T> apply(K key, SessionEntityWrapper<T> currentValue) {
        assert currentValue != null;
        return expectedVersion.equals(currentValue.getVersion()) ? newValue : currentValue;
    }

    @SuppressWarnings({"removal", "rawtypes"})
    private static class Externalizer implements AdvancedExternalizer<ReplaceFunction> {

        private static final SessionEntityWrapper.ExternalizerImpl EXTERNALIZER = new SessionEntityWrapper.ExternalizerImpl();
        private static final byte VERSION_1 = 1;

        @Override
        public Set<Class<? extends ReplaceFunction>> getTypeClasses() {
            return Set.of(ReplaceFunction.class);
        }

        @Override
        public Integer getId() {
            return Marshalling.REPLACE_FUNCTION_ID;
        }

        @Override
        public void writeObject(ObjectOutput output, ReplaceFunction object) throws IOException {
            output.writeByte(VERSION_1);
            MarshallUtil.marshallUUID(object.expectedVersion, output, false);
            EXTERNALIZER.writeObject(output, object.newValue);
        }

        @Override
        public ReplaceFunction<?,?> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            var version = input.readByte();
            if (version != VERSION_1) {
                throw new IOException("Invalid version: " + version);
            }
            //noinspection unchecked
            return new ReplaceFunction<Object, SessionEntity>(MarshallUtil.unmarshallUUID(input, false), EXTERNALIZER.readObject(input));
        }
    }
}
