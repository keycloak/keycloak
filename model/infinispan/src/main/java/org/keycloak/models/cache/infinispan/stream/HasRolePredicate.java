package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.CachedClient;
import org.keycloak.models.cache.infinispan.entities.CachedClientTemplate;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;
import org.keycloak.models.cache.infinispan.entities.CachedRole;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.entities.RoleQuery;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
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
            if (cachedRole.getRoleMappings().contains(role)) return true;
        }
        if (value instanceof RoleQuery) {
            RoleQuery roleQuery = (RoleQuery)value;
            if (roleQuery.getRoles().contains(role)) return true;
        }
        if (value instanceof CachedClient) {
            CachedClient cachedClient = (CachedClient)value;
            if (cachedClient.getScope().contains(role)) return true;

        }
        if (value instanceof CachedClientTemplate) {
            CachedClientTemplate cachedClientTemplate = (CachedClientTemplate)value;
            if (cachedClientTemplate.getScope().contains(role)) return true;

        }
        return false;
    }
}
