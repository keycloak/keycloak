package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.RealmQuery;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmQueryPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String realm;

    public static RealmQueryPredicate create() {
        return new RealmQueryPredicate();
    }

    public RealmQueryPredicate realm(String realm) {
        this.realm = realm;
        return this;
    }





    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof RealmQuery)) return false;
        RealmQuery query = (RealmQuery)value;


        return query.getRealms().contains(realm);
    }
}
