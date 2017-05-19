package org.keycloak.models.cache.infinispan.authorization.stream;

import org.keycloak.models.cache.infinispan.authorization.entities.InResourceServer;
import org.keycloak.models.cache.infinispan.entities.InRealm;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InResourceServerPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String serverId;

    public static InResourceServerPredicate create() {
        return new InResourceServerPredicate();
    }

    public InResourceServerPredicate resourceServer(String id) {
        serverId = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof InResourceServer)) return false;

        return serverId.equals(((InResourceServer)value).getResourceServerId());
    }
}
