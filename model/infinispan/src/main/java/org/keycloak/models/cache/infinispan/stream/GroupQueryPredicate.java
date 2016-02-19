package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.GroupQuery;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupQueryPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String group;

    public static GroupQueryPredicate create() {
        return new GroupQueryPredicate();
    }

    public GroupQueryPredicate group(String group) {
        this.group = group;
        return this;
    }





    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof GroupQuery)) return false;
        GroupQuery query = (GroupQuery)value;


        return query.getGroups().contains(group);
    }
}
