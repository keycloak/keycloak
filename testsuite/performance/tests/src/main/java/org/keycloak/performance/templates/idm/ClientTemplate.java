package org.keycloak.performance.templates.idm;

import org.keycloak.performance.dataset.idm.Client;
import org.keycloak.performance.dataset.idm.Realm;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.templates.NestedEntityTemplateWrapperList;
import org.keycloak.performance.templates.idm.authorization.ResourceServerTemplate;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ClientTemplate extends NestedEntityTemplate<Realm, Client, ClientRepresentation> {

    public static final String CLIENTS_PER_REALM = "clientsPerRealm";

    public final int clientsPerRealm;
    public final int clientsTotal;

    public final ClientRoleTemplate clientRoleTemplate;
    public final ResourceServerTemplate resourceServerTemplate;

    public ClientTemplate(RealmTemplate realmTemplate) {
        super(realmTemplate);

        this.clientsPerRealm = getConfiguration().getInt(CLIENTS_PER_REALM, 0);
        this.clientsTotal = clientsPerRealm * realmTemplate.realms;

        this.clientRoleTemplate = new ClientRoleTemplate(this);
        this.resourceServerTemplate = new ResourceServerTemplate(this);
    }

    public RealmTemplate realmTemplate() {
        return (RealmTemplate) getParentEntityTemplate();
    }

    @Override
    public int getEntityCountPerParent() {
        return clientsPerRealm;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s, total: %s", CLIENTS_PER_REALM, clientsPerRealm, clientsTotal));
        ValidateNumber.minValue(clientsPerRealm, 0);

        clientRoleTemplate.validateConfiguration();
        resourceServerTemplate.validateConfiguration();
    }

    @Override
    public Client newEntity(Realm parentEntity, int index) {
        return new Client(parentEntity, index);
    }

    @Override
    public void processMappings(Client client) {
        client.setClientRoles(new NestedEntityTemplateWrapperList<>(client, clientRoleTemplate));

        if (client.getRepresentation().getAuthorizationServicesEnabled()) {
            client.setResourceServer(resourceServerTemplate.produce(client));
        }
    }

}
