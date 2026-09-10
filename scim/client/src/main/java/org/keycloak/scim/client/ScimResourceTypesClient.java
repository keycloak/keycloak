package org.keycloak.scim.client;

import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.resourcetype.ResourceType;

public class ScimResourceTypesClient {

    private final ScimClient client;

    public ScimResourceTypesClient(ScimClient client) {
        this.client = client;
    }

    @SuppressWarnings("unchecked")
    public ListResponse<ResourceType> getAll() {
        return client.execute(client.doGet(ResourceType.class), ListResponse.class);
    }

    @SuppressWarnings("unchecked")
    public ListResponse<ResourceType> search(SearchRequest request) {
        return client.execute(client.doPost(ResourceType.class, "/.search").json(request), ListResponse.class);
    }
}
