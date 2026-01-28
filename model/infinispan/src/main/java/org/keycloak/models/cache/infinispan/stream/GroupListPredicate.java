package org.keycloak.models.cache.infinispan.stream;

import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.entities.GroupListQuery;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ProtoTypeId(Marshalling.GROUP_LIST_PREDICATE)
public class GroupListPredicate implements Predicate<Map.Entry<String, Revisioned>> {
    private String realm;

    public static GroupListPredicate create() {
        return new GroupListPredicate();
    }

    public GroupListPredicate realm(String realm) {
        this.realm = realm;
        return this;
    }

    @ProtoField(1)
    String getRealm() {
        return realm;
    }

    void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        return entry.getValue() instanceof GroupListQuery groupList && groupList.getRealm().equals(realm);
    }

}
