package org.keycloak.testframework.realm;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;

public class ManagedClientCleanup {

    private final List<ClientCleanup> cleanupTasks = new LinkedList<>();

    public ManagedClientCleanup add(ClientCleanup clientCleanup) {
        this.cleanupTasks.add(clientCleanup);
        return this;
    }

    void resetToOriginalRepresentation(ClientRepresentation rep) {
        if (cleanupTasks.stream().noneMatch(c -> c instanceof ResetClient)) {
            ClientRepresentation clone = RepresentationUtils.clone(rep);
            cleanupTasks.add(new ResetClient(clone));
        }
    }

    ClientRepresentation getOriginalRepresentation() {
        ResetClient clientCleanup = (ResetClient) cleanupTasks.stream().filter(c -> c instanceof ResetClient).findFirst().orElse(null);
        return clientCleanup != null ? clientCleanup.rep() : null;
    }

    void runCleanupTasks(ClientResource client) {
        cleanupTasks.forEach(t -> t.cleanup(client));
        cleanupTasks.clear();
    }

    public interface ClientCleanup {

        void cleanup(ClientResource client);

    }

    private record ResetClient(ClientRepresentation rep) implements ClientCleanup {

        @Override
        public void cleanup(ClientResource client) {
            client.update(rep);
        }

    }

}
