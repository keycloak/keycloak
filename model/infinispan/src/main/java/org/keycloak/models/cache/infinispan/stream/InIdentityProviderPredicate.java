package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.InIdentityProvider;
import org.keycloak.models.cache.infinispan.entities.InRealm;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@SerializeWith(InIdentityProviderPredicate.ExternalizerImpl.class)
public class InIdentityProviderPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String id;

    public static InIdentityProviderPredicate create() {
        return new InIdentityProviderPredicate();
    }

    public InIdentityProviderPredicate provider(String id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof InIdentityProvider)) return false;

        return ((InIdentityProvider)value).contains(id);
    }

    public static class ExternalizerImpl implements Externalizer<InIdentityProviderPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, InIdentityProviderPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.id, output);
        }

        @Override
        public InIdentityProviderPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public InIdentityProviderPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            InIdentityProviderPredicate res = new InIdentityProviderPredicate();
            res.id = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
