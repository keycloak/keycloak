package org.keycloak.scim.resource.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.utils.StringUtil.isBlank;

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
        if (!hasPermission(getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        return onCreate(resource);
    }

    @Override
    public R update(R resource) {
        M model = getModel(resource.getId());

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        populate(model, resource);

        return onUpdate(model, resource);
    }

    @Override
    public R get(String id) {
        return get(id, null, null);
    }

    public R get(String id, List<String> attributes, List<String> excludedAttributes) {
        M model = getModel(id);

        if (model == null) {
            return null;
        }

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.VIEW)) {
            throw new ForbiddenException();
        }

        R resource = createResourceTypeInstance();

        for (ModelSchema<M, R> schema : schemas) {
            schema.populate(resource, model, attributes, excludedAttributes);
        }

        return resource;
    }

    @Override
    public Stream<R> getAll(SearchRequest searchRequest) {
        if (!canQuery()) {
            throw new ForbiddenException();
        }

        return getModels(searchRequest).map(m -> {
            try {
                return get(m.getId(), searchRequest.getAttributes(), searchRequest.getExcludedAttributes());
            } catch (ForbiddenException fe) {
                return null;
            }
        }).filter(Objects::nonNull);
    }

    @Override
    public boolean delete(String id) {
        M model = getModel(id);

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        return onDelete(id);
    }

    @Override
    public void patch(R existing, List<PatchOperation> operations) {
        Objects.requireNonNull(existing, "existing cannot be null");
        Objects.requireNonNull(operations, "operations cannot be null");
        M model = getModel(existing.getId());

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        for (PatchOperation operation : operations) {
            String op = operation.getOp();

            if (isBlank(op)) {
                throw new ModelValidationException("Missing operation for patch operation");
            }

            String path = operation.getPath();
            JsonNode value = operation.getValue();

            for (ModelSchema<M, R> schema : schemas) {
                switch (op.toLowerCase()) {
                    case "add" -> schema.add(model, path, value);
                    case "replace" -> schema.replace(existing, model, path, value);
                    case "remove" -> schema.remove(existing, model, path);
                    default -> throw new RuntimeException("Unsupported patch operation " + op);
                }
            }
        }
    }

    @Override
    public String getSchema() {
        return schema.getId();
    }

    @Override
    public List<ModelSchema<M, R>> getSchemas() {
        return schemas;
    }

    @Override
    public List<String> getSchemaExtensions() {
        return schemaExtensions.stream().map(ModelSchema::getId).toList();
    }

    protected abstract R onCreate(R resource);

    protected abstract R onUpdate(M model, R resource);

    protected abstract boolean onDelete(String id);

    protected abstract Stream<M> getModels(SearchRequest searchRequest);

    protected abstract M getModel(String id);

    protected abstract String getRealmResourceType();

    protected void populate(M model, R resource) {
        for (ModelSchema<M, R> schema : schemas) {
            if (resource.hasSchema(schema.getId())) {
                schema.populate(model, resource);
            }
        }
    }

    private R createResourceTypeInstance() {
        try {
            return getResourceType().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create instance of resource type " + getResourceType(), e);
        }
    }

    private boolean canQuery() {
        return session.getContext().getPermissions().hasPermission(getRealmResourceType(), AdminPermissionsSchema.QUERY);
    }

    private boolean hasPermission(String realmResourceType, String scope) {
        return session.getContext().getPermissions().hasPermission(realmResourceType, scope);
    }

    private boolean hasPermission(M model, String realmResourceType, String scope) {
        return session.getContext().getPermissions().hasPermission(model, realmResourceType, scope);
    }

}
