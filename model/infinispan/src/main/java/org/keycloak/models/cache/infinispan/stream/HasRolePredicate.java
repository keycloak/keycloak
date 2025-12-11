package org.keycloak.models.cache.infinispan.stream;

import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.entities.CachedClient;
import org.keycloak.models.cache.infinispan.entities.CachedClientScope;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;
import org.keycloak.models.cache.infinispan.entities.CachedRole;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.entities.RoleQuery;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ProtoTypeId(Marshalling.HAS_ROLE_PREDICATE)
public class HasRolePredicate implements Predicate<Map.Entry<String, Revisioned>> {
    private String role;

    public static HasRolePredicate create() {
        return new HasRolePredicate();
    }

    public HasRolePredicate role(String role) {
        this.role = role;
        return this;
    }

    @ProtoField(1)
    String getRole() {
        return role;
    }

    void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        return (value instanceof CachedRole cachedRole && cachedRole.getComposites().contains(role)) ||
                (value instanceof CachedGroup cachedGroup && cachedGroup.getRoleMappings(null, null).contains(role)) ||
                (value instanceof RoleQuery roleQuery && roleQuery.getRoles().contains(role)) ||
                (value instanceof CachedClient cachedClient && cachedClient.getScope().contains(role)) ||
                (value instanceof CachedClientScope cachedClientScope && cachedClientScope.getScope().contains(role));
    }

}
