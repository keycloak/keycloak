package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.CachedClient;
import org.keycloak.models.cache.infinispan.entities.CachedClientScope;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;
import org.keycloak.models.cache.infinispan.entities.CachedRole;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.entities.RoleQuery;

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
@SerializeWith(HasRolePredicate.ExternalizerImpl.class)
public class HasRolePredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String role;

    public static HasRolePredicate create() {
        return new HasRolePredicate();
    }

    public HasRolePredicate role(String role) {
        this.role = role;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (value instanceof CachedRole) {
            CachedRole cachedRole = (CachedRole)value;
            if (cachedRole.getComposites().contains(role)) return true;
        }
        if (value instanceof CachedGroup) {
            CachedGroup cachedRole = (CachedGroup)value;
            if (cachedRole.getRoleMappings(null).contains(role)) return true;
        }
        if (value instanceof RoleQuery) {
            RoleQuery roleQuery = (RoleQuery)value;
            if (roleQuery.getRoles().contains(role)) return true;
        }
        if (value instanceof CachedClient) {
            CachedClient cachedClient = (CachedClient)value;
            if (cachedClient.getScope().contains(role)) return true;

        }
        if (value instanceof CachedClientScope) {
            CachedClientScope cachedClientScope = (CachedClientScope)value;
            if (cachedClientScope.getScope().contains(role)) return true;

        }
        return false;
    }

    public static class ExternalizerImpl implements Externalizer<HasRolePredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, HasRolePredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.role, output);
        }

        @Override
        public HasRolePredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public HasRolePredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            HasRolePredicate res = new HasRolePredicate();
            res.role = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
