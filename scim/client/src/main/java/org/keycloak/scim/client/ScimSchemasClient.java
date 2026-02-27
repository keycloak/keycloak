package org.keycloak.scim.client;

import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.schema.Schema;

/**
 * Client for interacting with the SCIM Schemas endpoint.
 * Schemas are read-only resources that describe the structure of SCIM resources.
 */
public class ScimSchemasClient extends AbstractScimResourceClient<Schema> {

    private final ScimClient scimClient;

    public ScimSchemasClient(ScimClient scimClient) {
        super(scimClient, Schema.class);
        this.scimClient = scimClient;
    }

    @SuppressWarnings("unchecked")
    private Class<ListResponse<Schema>> getListResponseType() {
        return (Class<ListResponse<Schema>>) (Class<?>) ListResponse.class;
    }

    /**
     * Retrieves all supported SCIM schemas.
     *
     * @return a ListResponse containing all schemas
     * @throws ScimClientException if the request fails
     */
    public ListResponse<Schema> getAll() {
        return scimClient.execute(doGet(""), getListResponseType());
    }

    /**
     * Schemas are read-only and cannot be created.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public Schema create(Schema resource) {
        throw new UnsupportedOperationException("Schemas are read-only and cannot be created");
    }

    /**
     * Schemas are read-only and cannot be updated.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public Schema update(Schema resource) {
        throw new UnsupportedOperationException("Schemas are read-only and cannot be updated");
    }

    /**
     * Schemas are read-only and cannot be deleted.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException("Schemas are read-only and cannot be deleted");
    }
}
