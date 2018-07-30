package org.keycloak.performance.dataset.idm;

import java.util.List;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.performance.dataset.Creatable;

/**
 *
 * @author tkyjovsk
 */
public class Client extends NestedEntity<Realm, ClientRepresentation>
        implements Creatable<ClientRepresentation> {

    private List<ClientRole> clientRoles;
    private ResourceServer resourceServer;

    public Client(Realm realm, int index) {
        super(realm, index);
    }

    @Override
    public ClientRepresentation newRepresentation() {
        return new ClientRepresentation();
    }

    @Override
    public String toString() {
        return getRepresentation().getClientId();
    }

    public Realm getRealm() {
        return getParentEntity();
    }

    public List<ClientRole> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(List<ClientRole> clientRoles) {
        this.clientRoles = clientRoles;
    }

    public ResourceServer getResourceServer() {
        return resourceServer;
    }

    public void setResourceServer(ResourceServer resourceServer) {
        this.resourceServer = resourceServer;
    }

    public synchronized ClientResource resource(Keycloak adminClient) {
        return getRealm().resource(adminClient).clients().get(getIdAndReadIfNull(adminClient));
    }

    @Override
    public synchronized ClientRepresentation read(Keycloak adminClient) {
        return getRealm().resource(adminClient).clients().findByClientId(getRepresentation().getClientId()).get(0);
    }

    @Override
    public synchronized Response create(Keycloak adminClient) {
        return getRealm().resource(adminClient).clients().create(getRepresentation());
    }

    @Override
    public synchronized void update(Keycloak adminClient) {
        resource(adminClient).update(getRepresentation());
    }

    @Override
    public synchronized void delete(Keycloak adminClient) {
        resource(adminClient).remove();
    }

}
