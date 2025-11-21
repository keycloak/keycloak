package org.keycloak.models.cache.infinispan.entities;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientListQuery extends AbstractRevisioned implements ClientQuery {
    private final Set<String> clients;
    private final String realm;

    public ClientListQuery(Long revisioned, String id, RealmModel realm, Set<String> clients) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.clients = clients;
    }

    public ClientListQuery(Long revisioned, String id, RealmModel realm, String client) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.clients = new HashSet<>();
        this.clients.add(client);
    }

    @Override
    public Set<String> getClients() {
        return clients;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        return "ClientListQuery{" +
                "id='" + getId() + "'" +
                "realm='" + realm + '\'' +
                '}';
    }
}
