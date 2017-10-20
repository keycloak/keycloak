package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.GroupListQuery;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
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
}
