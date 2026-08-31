package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;

public class ClientNodeRegistrationContext implements ClientModelContext {

    private final ClientModel client;
    private final String nodeHost;
    private final ClientPolicyEvent event;

    public ClientNodeRegistrationContext(ClientModel client,
                                         String nodeHost,
                                         ClientPolicyEvent event) {
        this.client = client;
        this.nodeHost = nodeHost;
        this.event = event;
    }

    @Override
    public ClientPolicyEvent getEvent() {
        return event;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    public String getNodeHost() {
        return nodeHost;
    }
}
