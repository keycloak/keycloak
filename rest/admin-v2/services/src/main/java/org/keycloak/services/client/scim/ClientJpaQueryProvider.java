package org.keycloak.services.client.scim;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.models.Model;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;

/**
 * Minimal {@link ScimResourceTypeProvider} adapter so {@link org.keycloak.scim.model.filter.ScimJPAPredicateEvaluator}
 * can resolve client query attributes. SCIM REST operations are not supported.
 */
public class ClientJpaQueryProvider implements ScimResourceTypeProvider<ClientQueryRepresentation> {

    @Override
    public String getSchema() {
        return ClientJpaQuerySchema.INSTANCE.getId();
    }

    @Override
    public List<ModelSchema<Model, ClientQueryRepresentation>> getSchemas() {
        return ClientJpaQuerySchema.SCHEMAS;
    }

    @Override
    public Class<ClientQueryRepresentation> getResourceType() {
        return ClientQueryRepresentation.class;
    }

    @Override
    public ClientQueryRepresentation create(ClientQueryRepresentation resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientQueryRepresentation update(ClientQueryRepresentation resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientQueryRepresentation get(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<ClientQueryRepresentation> getAll(SearchRequest searchRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count(SearchRequest searchRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }
}
