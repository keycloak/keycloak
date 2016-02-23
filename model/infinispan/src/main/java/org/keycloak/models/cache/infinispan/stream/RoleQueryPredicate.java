package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.entities.RoleQuery;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleQueryPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String role;

    public static RoleQueryPredicate create() {
        return new RoleQueryPredicate();
    }

    public RoleQueryPredicate role(String role) {
        this.role = role;
        return this;
    }





    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof RoleQuery)) return false;
        RoleQuery query = (RoleQuery)value;


        return query.getRoles().contains(role);
    }
}
