package org.keycloak.scim.resource.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;

public abstract class AbstractScimResourceTypeProvider<M extends Model, R extends ResourceTypeRepresentation> implements ScimResourceTypeProvider<R> {

    protected final KeycloakSession session;
    private final ModelSchema<M, R> schema;
    private final List<ModelSchema<M, R>> schemaExtensions;
    private final List<ModelSchema<M, R>> schemas;

    public AbstractScimResourceTypeProvider(KeycloakSession session, ModelSchema<M, R> schema, List<ModelSchema<M, R>> schemaExtensions) {
        this.session = session;
        this.schema = schema;
        this.schemaExtensions = schemaExtensions;
        this.schemas = new ArrayList<>();
        this.schemas.add(schema);
        this.schemas.addAll(schemaExtensions);
    }

    public AbstractScimResourceTypeProvider(KeycloakSession session, ModelSchema<M, R> schema) {
        this(session, schema, List.of());
    }

    @Override
    public R create(R resource) {
        KeycloakContext context = session.getContext();

        if (!context.hasPermission(getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        return onCreate(resource);
    }

    @Override
    public R update(R resource) {
        M model = getModel(resource.getId());

        KeycloakContext context = session.getContext();

        if (!context.hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        populate(model, resource);

        return onUpdate(model, resource);
    }

    @Override
    public R get(String id) {
        M model = getModel(id);

        if (model == null) {
            return null;
        }

        R resource = createResourceTypeInstance();

        KeycloakContext context = session.getContext();

        if (!context.hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        for (ModelSchema<M, R> schema : schemas) {
            schema.populate(resource, model);
        }

        return resource;
    }

    @Override
    public Stream<R> getAll(SearchRequest searchRequest) {
        return getModels(searchRequest).map(m -> {
            if (AdminPermissionsSchema.SCHEMA.isAdminPermissionsEnabled(session.getContext().getRealm())) {
                return get(m.getId());
            }
            try {
                return get(m.getId());
            } catch (ForbiddenException fe) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    @Override
    public boolean delete(String id) {
        M model = getModel(id);
        KeycloakContext context = session.getContext();

        if (!context.hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        return onDelete(id);
    }

    @Override
    public String getSchema() {
        return schema.getName();
    }

    @Override
    public List<String> getSchemaExtensions() {
        return schemaExtensions.stream().map(ModelSchema::getName).toList();
    }

    protected abstract R onCreate(R resource);

    protected abstract R onUpdate(M model, R resource);

    protected abstract boolean onDelete(String id);

    protected abstract Stream<M> getModels(SearchRequest searchRequest);

    protected abstract M getModel(String id);

    protected abstract String getRealmResourceType();

    protected void populate(M model, R resource) {
        for (ModelSchema<M, R> schema : schemas) {
            if (resource.hasSchema(schema.getName())) {
                schema.populate(model, resource);
            }
        }
    }

    protected String[] splitScimAttribute(String scimAttrPath) {
        // first split the attribute path into schema and attribute name. If no schema is specified, use the core user schema by default
        String schemaName;
        int lastColon = scimAttrPath.lastIndexOf(':');
        if (lastColon > 0 && (scimAttrPath.contains("://") || scimAttrPath.startsWith("urn:"))) {
            schemaName = scimAttrPath.substring(0, lastColon);
            scimAttrPath = scimAttrPath.substring(lastColon + 1);
        } else {
            schemaName = this.schema.getName();
        }
        return new String[] {schemaName, scimAttrPath};
    }

    private R createResourceTypeInstance() {
        try {
            return (R) getResourceType().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create instance of resource type " + getResourceType(), e);
        }
    }
}
