package org.keycloak.performance.dataset.idm.authorization;

import java.util.List;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientPoliciesResource;
import org.keycloak.admin.client.resource.ClientPolicyResource;
import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ClientPolicy extends Policy<ClientPolicyRepresentation> {

    private List<Client> clients;

    public ClientPolicy(ResourceServer resourceServer, int index) {
        super(resourceServer, index);
    }

    @Override
    public ClientPolicyRepresentation newRepresentation() {
        return new ClientPolicyRepresentation();
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    public ClientPoliciesResource clientPoliciesResource(Keycloak adminClient) {
        return policies(adminClient).client();
    }

    public ClientPolicyResource resource(Keycloak adminClient) {
        return getResourceServer().resource(adminClient).policies().client().findById(getIdAndReadIfNull(adminClient));
    }

    @Override
    public ClientPolicyRepresentation read(Keycloak adminClient) {
        return clientPoliciesResource(adminClient).findByName(getRepresentation().getName());
    }

    @Override
    public Response create(Keycloak adminClient) {
        return clientPoliciesResource(adminClient).create(getRepresentation());
    }

    @Override
    public void update(Keycloak adminClient) {
        resource(adminClient).update(getRepresentation());
    }

    @Override
    public void delete(Keycloak adminClient) {
        resource(adminClient).remove();
    }

}
