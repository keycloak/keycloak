package org.keycloak.scim.client;

import java.io.IOException;

import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.ScimResource;
import org.keycloak.scim.resource.ScimResource.Type;

import org.apache.http.HttpStatus;

import static java.util.Objects.requireNonNull;

public abstract class AbstractScimResourceClient<R extends ScimResource> implements AutoCloseable {

    private final ScimClient client;
    protected final Type resourceType;

    public AbstractScimResourceClient(ScimClient client, Type resourceType) {
        this.client = client;
        this.resourceType = resourceType;
    }

    public R create(R resource) {
        requireNonNull(resource, "SCIM resource must not be null");

        if (!resource.getType().equals(resourceType)) {
            throw new IllegalArgumentException("SCIM object does not match resource type " + resourceType);
        }

        return (R) execute(client.doPost(resourceType.getPath()).json(resource), resource.getClass());
    }

    public R update(R resource) {
        requireNonNull(resource, "SCIM resource must not be null");

        if (!resource.getType().equals(resourceType)) {
            throw new IllegalArgumentException("SCIM object does not match resource type " + resourceType);
        }

        return (R) execute(client.doPut(resourceType.getPath() + "/" + resource.getId())
                .json(resource), resource.getClass());
    }

    public void delete(String id) {
        requireNonNull(id, "SCIM resource ID must not be null");
        execute(client.doDelete(resourceType.getPath() + "/" + id));
    }

    public R get(String id) {
        requireNonNull(id, "SCIM resource ID must not be null");

        try {
            return (R) execute(doGet("/" + id), resourceType.getType());
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

    protected ListResponse<R> doFilter(ResourceFilter filter) {
        SimpleHttpRequest request = doGet("");
        String query = filter.build();

        if (!query.isEmpty()) {
            request = request.param("filter", query);
        }

        return execute(request, ListResponse.class);
    }

    protected SimpleHttpRequest doGet(String path) {
        return client.doGet(resourceType.getPath() + path);
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    private <T> T execute(SimpleHttpRequest request, Class<T> responseType) {
        try (SimpleHttpResponse response = client.execute(request)) {
            if (responseType == null) {
                return null;
            }
            return response.asJson(responseType);
        } catch (IOException e) {
            throw new ScimClientException("Error executing request", e);
        }
    }

    private void execute(SimpleHttpRequest request) {
        execute(request, null);
    }
}
