package org.keycloak.scim.resource.spi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.keycloak.scim.resource.schema.attribute.Attribute;

import com.fasterxml.jackson.databind.JsonNode;

import static java.util.function.Predicate.not;

import static org.keycloak.utils.StringUtil.isBlank;

public abstract class AbstractScimResourceTypeProvider<M extends Model, R extends ResourceTypeRepresentation> implements ScimResourceTypeProvider<R> {

    /**
     * Maximum number of operations allowed in a single SCIM PATCH request.
     * Exceeding this limit results in a {@code 400 Bad Request} with {@code scimType=tooMany}.
     * This limit is not advertised via {@code /ServiceProviderConfig}.
     */
    public static final int MAX_PATCH_OPERATIONS = 100;

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

        return createResourceTypeInstance(model, attributes, excludedAttributes);
    }

    @Override
    public Stream<R> getAll(SearchRequest searchRequest) {
        if (!canQuery()) {
            throw new ForbiddenException();
        }

        return getModels(searchRequest)
                .map(m -> createResourceTypeInstance(m, searchRequest.getAttributes(), searchRequest.getExcludedAttributes()));
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

        if (operations.size() > MAX_PATCH_OPERATIONS) {
            throw new ScimPatchException(
                    "PATCH request exceeds maximum allowed number of %d operations".formatted(MAX_PATCH_OPERATIONS));
        }

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

            if (!isBlank(path)) {
                validatePatchPath(path);
            }

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
    public List<ModelSchema<M, R>> getSchemas() {
        return schemas;
    }

    @Override
    public List<String> getSchemaExtensions() {
        return schemaExtensions.stream().filter(not(ModelSchema::isInternal)).map(ModelSchema::getId).toList();
    }

    protected abstract R onCreate(R resource);

    protected abstract R onUpdate(M model, R resource);

    protected abstract boolean onDelete(String id);

    protected abstract Stream<M> getModels(SearchRequest searchRequest);

    protected abstract M getModel(String id);

    protected abstract String getRealmResourceType();

    protected void populate(M model, R resource) {
        validateSchemas(resource);
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

    private boolean canQuery() {
        return session.getContext().getPermissions().hasPermission(getRealmResourceType(), AdminPermissionsSchema.QUERY)
                || session.getContext().getPermissions().hasPermission(getRealmResourceType(), AdminPermissionsSchema.VIEW);
    }

    private boolean hasPermission(String realmResourceType, String scope) {
        return session.getContext().getPermissions().hasPermission(realmResourceType, scope);
    }

    protected boolean hasPermission(M model, String realmResourceType, String scope) {
        if (AdminPermissionsSchema.VIEW.equals(scope)) {
            return session.getContext().getPermissions().hasPermission(model, realmResourceType, scope);
        }

        return session.getContext().getPermissions().hasPermission(model, realmResourceType, scope) && isManageable(model);
    }

    protected boolean isManageable(M model) {
        return true;
    }

    /**
     * Validates that all schema URIs in the request's {@code schemas} array and all keys in the
     * {@code extensions} map are recognized by at least one registered schema. Throws
     * {@link ScimInvalidValueException} (HTTP 400, {@code scimType=invalidValue}) if any are not.
     *
     * <p>Called from {@link #populate(Model, ResourceTypeRepresentation)} to validate POST and PUT requests.
     */
    private void validateSchemas(R resource) {
        Set<String> recognized = getRecognizedSchemaUris();

        Set<String> requestSchemas = resource.getSchemas();
        if (requestSchemas != null) {
            for (String uri : requestSchemas) {
                if (!recognized.contains(uri)) {
                    throw new ScimInvalidValueException(
                            "Schema URI '" + uri + "' is not recognized");
                }
            }
        }

        Map<String, Object> extensions = resource.getExtensions();
        if (extensions != null) {
            for (String key : extensions.keySet()) {
                if (!recognized.contains(key)) {
                    throw new ScimInvalidValueException(
                            "Unrecognized attribute '" + key + "'");
                }
            }
        }
    }

    /**
     * Validates that a PATCH operation path targets a recognized attribute. The path is first normalized
     * by stripping any filter expressions (e.g. {@code emails[type eq "work"].value} becomes {@code emails.value}),
     * then checked against recognized schema URIs, attribute paths, and attribute name prefixes. Throws
     * {@link ScimNoTargetException} (HTTP 400, {@code scimType=noTarget}) if the path is not recognized.
     */
    private void validatePatchPath(String rawPath) {
        String normalizedPath = normalizePath(rawPath);

        Set<String> recognized = getRecognizedSchemaUris();
        if (recognized.contains(normalizedPath)) {
            return;
        }

        for (ModelSchema<M, R> s : schemas) {
            if (s.getAttributeByPath(normalizedPath) != null) {
                return;
            }
            for (Attribute<M, R> attr : s.getAttributes().values()) {
                String attrName = attr.getName();
                String parentName = attr.getParentName();
                if (normalizedPath.equalsIgnoreCase(attrName)
                        || normalizedPath.toLowerCase().startsWith(attrName.toLowerCase() + ".")
                        || (parentName != null && (normalizedPath.equalsIgnoreCase(parentName)
                            || normalizedPath.toLowerCase().startsWith(parentName.toLowerCase() + ".")))) {
                    return;
                }
            }
        }

        throw new ScimNoTargetException(
                "The path '" + rawPath + "' does not target a recognized attribute");
    }

    /**
     * Strips filter expressions from a PATCH path so that only the structural attribute path remains.
     * For example, {@code emails[type eq "work"].value} becomes {@code emails.value}.
     */
    private String normalizePath(String rawPath) {
        int filterStart = rawPath.indexOf('[');
        if (filterStart > 0) {
            int filterEnd = rawPath.lastIndexOf(']');
            if (filterEnd == -1) {
                return rawPath;
            }
            String prefix = rawPath.substring(0, filterStart);
            int dotAfterFilter = rawPath.indexOf('.', filterEnd);
            if (dotAfterFilter != -1) {
                return prefix + rawPath.substring(filterEnd + 1);
            }
            return prefix;
        }
        return rawPath;
    }

    /**
     * Collects the set of all schema URIs recognized by this provider. This includes the {@code id} of
     * each registered {@link ModelSchema} (core and extensions) as well as the schema URI extracted from
     * each attribute via {@link Attribute#getSchema()}. The latter is important for custom extension schemas
     * that are dynamically registered through user profile attribute annotations - their schema URIs only
     * appear as part of the attribute name (e.g. {@code urn:custom:schema:User:myAttr} yields
     * {@code urn:custom:schema:User}) and would otherwise not be present in the recognized set.
     */
    private Set<String> getRecognizedSchemaUris() {
        Set<String> uris = new HashSet<>();
        for (ModelSchema<M, R> s : schemas) {
            uris.add(s.getId());
            for (Attribute<M, R> attr : s.getAttributes().values()) {
                String attrSchema = attr.getSchema();
                if (attrSchema != null) {
                    uris.add(attrSchema);
                }
            }
        }
        return uris;
    }
}
