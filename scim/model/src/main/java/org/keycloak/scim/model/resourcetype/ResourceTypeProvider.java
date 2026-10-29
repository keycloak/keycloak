package org.keycloak.scim.model.resourcetype;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.config.ServiceProviderConfig;
import org.keycloak.scim.resource.resourcetype.ResourceType;
import org.keycloak.scim.resource.resourcetype.ResourceType.SchemaExtension;
import org.keycloak.scim.resource.schema.Schema;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.scim.resource.spi.ScimResourceTypeProviderFactory;
import org.keycloak.scim.resource.spi.SearchOptions;
import org.keycloak.utils.StringUtil;

import static org.keycloak.scim.resource.Scim.hasDiscoveryEndpointPermission;

public class ResourceTypeProvider implements ScimResourceTypeProvider<ResourceType> {

    private static final List<Class<? extends ResourceTypeRepresentation>> EXCLUDED_RESOURCE_TYPES = List.of(ServiceProviderConfig.class, ResourceType.class, Schema.class);
    private final KeycloakSession session;

    public ResourceTypeProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
    }

    @Override
    public Class<ResourceType> getResourceType() {
        return ResourceType.class;
    }

    @Override
    public ResourceType create(ResourceType resource) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResourceType update(ResourceType resourceType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResourceType get(String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Stream<ResourceType> getAll(SearchOptions searchRequest) {
        // If searchRequest.filter is present, the provider should respond with forbidden status to ensure that clients cannot incorrectly assume that any matching conditions specified in a filter are true.
        if (hasDiscoveryEndpointPermission(session) && (searchRequest == null || StringUtil.isBlank(searchRequest.getFilter()))) {
            return session.getKeycloakSessionFactory().getProviderFactoriesStream(ScimResourceTypeProvider.class)
                    .map(ScimResourceTypeProviderFactory.class::cast)
                    .map(this::toRepresentation)
                    .filter(Objects::nonNull);
        }
        throw new ForbiddenException();
    }

    @Override
    public Long count(SearchOptions searchOptions, int resourceSize) {
        return (long) resourceSize;
    }

    private ResourceType toRepresentation(ScimResourceTypeProviderFactory<? extends ScimResourceTypeProvider<? extends ResourceTypeRepresentation>> factory) {
        ScimResourceTypeProvider<? extends ResourceTypeRepresentation> provider = factory.create(session);

        if (EXCLUDED_RESOURCE_TYPES.contains(provider.getResourceType())) {
            return null;
        }

        ResourceType representation = new ResourceType();
        ResourceTypeRepresentation resourceType;

        try {
            resourceType = provider.getResourceType().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not instantiate resource type representation for provider " + factory.getId(), e);
        }

        representation.setId(provider.getName());
        representation.setName(provider.getName());
        representation.setDescription(provider.getDescription());
        representation.setEndpoint("/" + factory.getId());
        representation.setSchema(provider.getSchema());

        List<SchemaExtension> schemaExtensions = new ArrayList<>();

        for (String name : provider.getSchemaExtensions()) {
            SchemaExtension extension = new SchemaExtension();
            extension.setSchema(name);
            schemaExtensions.add(extension);
        }

        representation.setSchemaExtensions(schemaExtensions);

        return representation;
    }

    @Override
    public boolean delete(String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getSchema() {
        return ResourceType.SCHEMA;
    }

}
