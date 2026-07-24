package org.keycloak.scim.client;


import org.keycloak.scim.resource.config.ServiceProviderConfig;

public class ScimConfigClient {

    private final ScimClient client;

    public ScimConfigClient(ScimClient client) {
        this.client = client;
    }

    public ServiceProviderConfig get() {
        return client.execute(client.doGet(ServiceProviderConfig.class), ServiceProviderConfig.class);
    }
}
