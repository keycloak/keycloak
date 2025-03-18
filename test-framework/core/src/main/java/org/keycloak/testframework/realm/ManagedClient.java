package org.keycloak.testframework.realm;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.injection.ManagedTestResource;

public class ManagedClient extends ManagedTestResource {

    private final ClientRepresentation createdRepresentation;
    private final ClientResource clientResource;

    private ManagedClientCleanup cleanup;

    public ManagedClient(ClientRepresentation createdRepresentation, ClientResource clientResource) {
        this.createdRepresentation = createdRepresentation;
        this.clientResource = clientResource;
    }

    public String getId() {
        return createdRepresentation.getId();
    }

    public String getClientId() {
        return createdRepresentation.getClientId();
    }

    public String getSecret() {
        return createdRepresentation.getSecret();
    }

    public ClientResource admin() {
        return clientResource;
    }

    public void updateWithCleanup(ManagedClient.ClientUpdate... updates) {
        ClientRepresentation rep = admin().toRepresentation();
        cleanup().resetToOriginalRepresentation(rep);

        ClientConfigBuilder configBuilder = ClientConfigBuilder.update(rep);
        for (ManagedClient.ClientUpdate update : updates) {
            configBuilder = update.update(configBuilder);
        }

        ClientRepresentation updated = configBuilder.build();
        admin().update(updated);

        ClientRepresentation original = cleanup().getOriginalRepresentation();
        updated.getAttributes().keySet().stream().filter(k -> !original.getAttributes().containsKey(k)).forEach(k -> original.getAttributes().put(k, ""));
    }

    public ManagedClientCleanup cleanup() {
        if (cleanup == null) {
            cleanup = new ManagedClientCleanup();
        }
        return cleanup;
    }

    @Override
    public void runCleanup() {
        if (cleanup != null) {
            cleanup.runCleanupTasks(clientResource);
            cleanup = null;
        }
    }

    public interface ClientUpdate {

        ClientConfigBuilder update(ClientConfigBuilder client);

    }

}
