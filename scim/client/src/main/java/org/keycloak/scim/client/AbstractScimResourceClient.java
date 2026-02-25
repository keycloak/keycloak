package org.keycloak.scim.client;


import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.ResourceTypeRepresentation;

import org.apache.http.HttpStatus;

import static java.util.Objects.requireNonNull;


public abstract class AbstractScimResourceClient<R extends ResourceTypeRepresentation> implements AutoCloseable {

    protected final ScimClient client;
    private final Class<R> resourceTypeClass;

    public AbstractScimResourceClient(ScimClient client, Class<R> resourceType) {
        this.client = client;
        this.resourceTypeClass = resourceType;
    }

    public R create(R resource) {
        requireNonNull(resource, "SCIM resource must not be null");
        return client.execute(client.doPost(resourceTypeClass).json(resource), resourceTypeClass);
    }

    public R update(R resource) {
        requireNonNull(resource, "SCIM resource must not be null");
        return client.execute(client.doPut(resourceTypeClass, resource.getId())
                .json(resource), resourceTypeClass);
    }

    public void delete(String id) {
        requireNonNull(id, "SCIM resource ID must not be null");
        client.execute(client.doDelete(resourceTypeClass, id));
    }

    public R get(String id) {
        requireNonNull(id, "SCIM resource ID must not be null");

        try {
            return client.execute(doGet("/" + id), resourceTypeClass);
        } catch (ScimClientException scime) {
            ErrorResponse error = scime.getError();

            if (error != null) {
                if (HttpStatus.SC_NOT_FOUND == error.getStatusInt()) {
                    return null;
                }
            }

            throw scime;
        }
    }

    @SuppressWarnings("unchecked")
    protected ListResponse<R> doFilter(ResourceFilter filter) {
        SimpleHttpRequest request = doGet("");
        String query = filter.build();

        if (!query.isEmpty()) {
            request = request.param("filter", query);
        }

        return client.execute(request, ListResponse.class);
    }

    protected SimpleHttpRequest doGet(String path) {
        return client.doGet(resourceTypeClass, path);
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}
