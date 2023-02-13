package org.keycloak.models.cache.infinispan.authorization.stream;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.models.cache.infinispan.authorization.entities.InScope;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SerializeWith(InScopePredicate.ExternalizerImpl.class)
public class InScopePredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String scopeId;

    public static InScopePredicate create() {
        return new InScopePredicate();
    }

    public InScopePredicate scope(String id) {
        scopeId = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof InScope)) return false;

        return scopeId.equals(((InScope)value).getScopeId());
    }

    public static class ExternalizerImpl implements Externalizer<InScopePredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, InScopePredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.scopeId, output);
        }

        @Override
        public InScopePredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public InScopePredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            InScopePredicate res = new InScopePredicate();
            res.scopeId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
