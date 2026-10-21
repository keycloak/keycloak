package org.keycloak.models.cache.infinispan.stream;

import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.entities.InRealm;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ProtoTypeId(Marshalling.IN_REALM_PREDICATE)
public class InRealmPredicate implements Predicate<Map.Entry<String, Revisioned>> {
    private String realm;

    public static InRealmPredicate create() {
        return new InRealmPredicate();
    }

    public InRealmPredicate realm(String id) {
        realm = id;
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
        return entry.getValue() instanceof InRealm inRealm && realm.equals(inRealm.getRealm());

    }

}
