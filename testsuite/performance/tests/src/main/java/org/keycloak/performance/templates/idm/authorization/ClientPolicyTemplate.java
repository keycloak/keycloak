package org.keycloak.performance.templates.idm.authorization;

import static java.util.stream.Collectors.toSet;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.dataset.idm.authorization.ClientPolicy;
import org.keycloak.performance.iteration.RandomSublist;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ClientPolicyTemplate extends PolicyTemplate<ClientPolicy, ClientPolicyRepresentation> {

    public static final String CLIENT_POLICIES_PER_RESOURCE_SERVER = "clientPoliciesPerResourceServer";
    public static final String CLIENTS_PER_CLIENT_POLICY = "clientsPerClientPolicy";

    public final int clientPoliciesPerResourceServer;
    public final int clientsPerClientPolicy;

    public ClientPolicyTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
        this.clientPoliciesPerResourceServer = getConfiguration().getInt(CLIENT_POLICIES_PER_RESOURCE_SERVER, 0);
        this.clientsPerClientPolicy = getConfiguration().getInt(CLIENTS_PER_CLIENT_POLICY, 0);
    }

    @Override
    public int getEntityCountPerParent() {
        return clientPoliciesPerResourceServer;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s", CLIENT_POLICIES_PER_RESOURCE_SERVER, clientPoliciesPerResourceServer));
        ValidateNumber.minValue(clientPoliciesPerResourceServer, 0);

        logger().info(String.format("%s: %s", CLIENTS_PER_CLIENT_POLICY, clientsPerClientPolicy));
        ValidateNumber.isInRange(clientsPerClientPolicy, 0,
                resourceServerTemplate().clientTemplate().realmTemplate().clientTemplate.clientsPerRealm);
    }

    @Override
    public ClientPolicy newEntity(ResourceServer parentEntity, int index) {
        return new ClientPolicy(parentEntity, index);
    }

    @Override
    public void processMappings(ClientPolicy policy) {
        policy.setClients(new RandomSublist<>(
                policy.getResourceServer().getClient().getRealm().getClients(), // original list
                policy.hashCode(), // random seed
                clientsPerClientPolicy, // sublist size
                false // unique randoms?
        ));
        policy.getRepresentation().setClients(policy.getClients()
                .stream().map(u -> u.getId())
                .filter(id -> id != null) // need non-null policy IDs
                .collect(toSet()));
    }

}
