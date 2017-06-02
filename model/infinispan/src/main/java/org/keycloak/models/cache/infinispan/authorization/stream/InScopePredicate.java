package org.keycloak.models.cache.infinispan.authorization.stream;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.models.cache.infinispan.authorization.entities.InResourceServer;
import org.keycloak.models.cache.infinispan.authorization.entities.InScope;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
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
}
