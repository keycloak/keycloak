package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.InClient;
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
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SerializeWith(InClientPredicate.ExternalizerImpl.class)
public class InClientPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String clientId;

    public static InClientPredicate create() {
        return new InClientPredicate();
    }

    public InClientPredicate client(String id) {
        clientId = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof InClient)) return false;

        return clientId.equals(((InClient)value).getClientId());
    }

    public static class ExternalizerImpl implements Externalizer<InClientPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, InClientPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.clientId, output);
        }

        @Override
        public InClientPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public InClientPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            InClientPredicate res = new InClientPredicate();
            res.clientId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
