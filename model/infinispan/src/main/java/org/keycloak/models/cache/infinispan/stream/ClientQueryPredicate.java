package org.keycloak.models.cache.infinispan.stream;

import org.jboss.logging.Logger;
import org.keycloak.models.cache.infinispan.entities.ClientQuery;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientQueryPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    protected static final Logger logger = Logger.getLogger(ClientQueryPredicate.class);
    private String client;
    private String inRealm;

    public static ClientQueryPredicate create() {
        return new ClientQueryPredicate();
    }

    public ClientQueryPredicate client(String client) {
        this.client = client;
        return this;
    }

    public ClientQueryPredicate inRealm(String inRealm) {
        this.inRealm = inRealm;
        return this;
    }





    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof ClientQuery)) return false;
        ClientQuery query = (ClientQuery)value;
        if (client != null && !query.getClients().contains(client)) return false;
        if (inRealm != null && !query.getRealm().equals(inRealm)) return false;
        return true;
    }
}
