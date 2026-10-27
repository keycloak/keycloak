package org.keycloak.scim.resource.spi;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;

import com.fasterxml.jackson.databind.JsonNode;

import static java.util.function.Predicate.not;

import static org.keycloak.utils.StringUtil.isBlank;

public abstract class AbstractScimResourceTypeProvider<M extends Model, R extends ResourceTypeRepresentation> extends BaseResourceTypeProvider<M, R> {

    /**
     * Maximum number of operations allowed in a single SCIM PATCH request.
     * Exceeding this limit results in a {@code 400 Bad Request} with {@code scimType=tooMany}.
     * This limit is not advertised via {@code /ServiceProviderConfig}.
     */
    public static final int MAX_PATCH_OPERATIONS = 100;

    private final ModelSchema<M, R> schema;
    private final List<ModelSchema<M, R>> schemaExtensions;

    public AbstractScimResourceTypeProvider(KeycloakSession session, ModelSchema<M, R> schema, List<ModelSchema<M, R>> schemaExtensions) {
        super(session, Stream.concat(Stream.of(schema), schemaExtensions.stream()));
        this.schema = schema;
        this.schemaExtensions = schemaExtensions;
    }

    public AbstractScimResourceTypeProvider(KeycloakSession session, ModelSchema<M, R> schema) {
        this(session, schema, List.of());
    }
    
    @Override
    protected String getModelId(R resource) {
        return resource.getId();
    }

    @Override
    public void patch(R existing, List<PatchOperation> operations) {
        Objects.requireNonNull(existing, "existing cannot be null");
        Objects.requireNonNull(operations, "operations cannot be null");

        if (operations.size() > MAX_PATCH_OPERATIONS) {
            throw new ScimPatchException(
                    "PATCH request exceeds maximum allowed number of %d operations".formatted(MAX_PATCH_OPERATIONS));
        }

        M model = getModel(existing.getId());

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        applyPatch(existing, model, operations);
    }

    protected void applyPatch(R existing, M model, List<PatchOperation> operations) {
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
                    default -> throw new ModelValidationException("Unsupported patch operation " + op);
                }
            }
        }
    }

    @Override
    public String getSchema() {
        return schema.getId();
    }
    
    @Override
    public List<String> getSchemaExtensions() {
        return schemaExtensions.stream().filter(not(ModelSchema::isInternal)).map(ModelSchema::getId).toList();
    }

    protected void populate(M model, R resource) {
        for (ModelSchema<M, R> schema : schemas) {
            if (schema.supports(resource.getSchemas())) {
                schema.populate(model, resource);
            }
        }
    }

    protected R createResourceTypeInstance(M model, List<String> attributes, List<String> excludedAttributes) {
        try {
            R resource = getResourceType().getDeclaredConstructor().newInstance();

            for (ModelSchema<M, R> schema : schemas) {
                schema.populate(resource, model, attributes, excludedAttributes);
            }

            return resource;
        } catch (Exception e) {
            throw new RuntimeException("Could not create instance of resource type " + getResourceType(), e);
        }
    }

}
