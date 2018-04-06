package org.keycloak.models.cache.infinispan.stream;

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
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SerializeWith(InRealmPredicate.ExternalizerImpl.class)
public class InRealmPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String realm;

    public static InRealmPredicate create() {
        return new InRealmPredicate();
    }

    public InRealmPredicate realm(String id) {
        realm = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof InRealm)) return false;

        return realm.equals(((InRealm)value).getRealm());
    }

    public static class ExternalizerImpl implements Externalizer<InRealmPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, InRealmPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.realm, output);
        }

        @Override
        public InRealmPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public InRealmPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            InRealmPredicate res = new InRealmPredicate();
            res.realm = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
