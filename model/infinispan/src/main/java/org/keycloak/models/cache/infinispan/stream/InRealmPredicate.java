package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.InRealm;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
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
}
