package org.keycloak.models.cache.infinispan.stream;

import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.entities.InClient;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ProtoTypeId(Marshalling.IN_CLIENT_PREDICATE)
public class InClientPredicate implements Predicate<Map.Entry<String, Revisioned>> {
    private String clientId;

    public static InClientPredicate create() {
        return new InClientPredicate();
    }

    public InClientPredicate client(String id) {
        clientId = id;
        return this;
    }

    @ProtoField(1)
    String getClientId() {
        return clientId;
    }

    void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        return entry.getValue() instanceof InClient inClient && clientId.equals(inClient.getClientId());
    }

}
