package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.InClient;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InClientPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String clientId;

    public static InClientPredicate create() {
        return new InClientPredicate();
    }

    public InClientPredicate client(String id) {
        clientId = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof InClient)) return false;

        return clientId.equals(((InClient)value).getClientId());
    }
}
