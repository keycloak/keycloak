package org.keycloak.models.cache.infinispan.authorization.stream;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.authorization.entities.InResourceServer;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ProtoTypeId(Marshalling.IN_RESOURCE_SERVER_PREDICATE)
public class InResourceServerPredicate implements Predicate<Map.Entry<String, Revisioned>> {
    private final String serverId;

    private InResourceServerPredicate(String serverId) {
        this.serverId = Objects.requireNonNull(serverId);
    }

    @ProtoFactory
    public static InResourceServerPredicate create(String serverId) {
        return new InResourceServerPredicate(serverId);
    }

    @ProtoField(1)
    String getServerId() {
        return serverId;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        return entry.getValue() instanceof InResourceServer inResourceServer && serverId.equals(inResourceServer.getResourceServerId());
    }

}
