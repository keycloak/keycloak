package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.GroupListQuery;
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
@SerializeWith(GroupListPredicate.ExternalizerImpl.class)
public class GroupListPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String realm;

    public static GroupListPredicate create() {
        return new GroupListPredicate();
    }

    public GroupListPredicate realm(String realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (value instanceof GroupListQuery) {
            GroupListQuery groupList = (GroupListQuery)value;
            if (groupList.getRealm().equals(realm)) return true;
        }
        return false;
    }

    public static class ExternalizerImpl implements Externalizer<GroupListPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, GroupListPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.realm, output);
        }

        @Override
        public GroupListPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public GroupListPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            GroupListPredicate res = new GroupListPredicate();
            res.realm = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
