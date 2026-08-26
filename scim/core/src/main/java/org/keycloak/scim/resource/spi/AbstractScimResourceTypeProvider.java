package org.keycloak.scim.resource.spi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import static java.util.function.Predicate.not;

import static org.keycloak.utils.StringUtil.isBlank;

public abstract class AbstractScimResourceTypeProvider<M extends Model, R extends ResourceTypeRepresentation> extends BaseResourceTypeProvider<M, R> {

    /**
     * Maximum number of operations allowed in a single SCIM PATCH request.
     * Exceeding this limit results in a {@code 400 Bad Request} with {@code scimType=tooMany}.
     * This limit is not advertised via {@code /ServiceProviderConfig}.
     */
    public static final int MAX_PATCH_OPERATIONS = 100;

    /**
     * Cache of the canonical sub-attribute names of a complex attribute's backing Java type, keyed by that
     * type. Populated lazily via Jackson bean introspection (see {@link #getComplexSubAttributes(Class)}).
     */
    private static final Map<Class<?>, Set<String>> COMPLEX_SUB_ATTRIBUTES = new ConcurrentHashMap<>();

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

        for (PatchOperation operation : operations) {
            String op = operation.getOp();

            if (isBlank(op)) {
                throw new ModelValidationException("Missing operation for patch operation");
            }

            String path = operation.getPath();
            JsonNode value = operation.getValue();

            if (!isBlank(path)) {
                validatePatchPath(path);
            } else if (value != null && value.isObject()) {
                validatePatchValue(value);
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
    public List<String> getSchemaExtensions() {
        return schemaExtensions.stream().filter(not(ModelSchema::isInternal)).map(ModelSchema::getId).toList();
    }

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
            for (Map.Entry<String, Object> entry : extensions.entrySet()) {
                String key = entry.getKey();
                if (!recognized.contains(key)) {
                    throw new ScimInvalidValueException(
                            "Unrecognized attribute '" + key + "'");
                }

                // the schema URI is recognized; validate the nested attributes it carries so that e.g.
                // {"urn:custom:schema:User": {"bogus": "x"}} is rejected rather than silently ignored
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> nested) {
                    for (Object nestedKey : nested.keySet()) {
                        String qualified = key + ":" + nestedKey;
                        if (!isRecognizedPath(qualified)) {
                            throw new ScimInvalidValueException(
                                    "Unrecognized attribute '" + qualified + "'");
                        }
                    }
                }
            }
        }
    }

    /**
     * Validates that a PATCH operation path targets a recognized attribute, throwing
     * {@link ScimNoTargetException} (HTTP 400, {@code scimType=noTarget}) if it does not.
     *
     * @see #isRecognizedPath(String)
     */
    private void validatePatchPath(String rawPath) {
        if (!isRecognizedPath(rawPath)) {
            throw new ScimNoTargetException(
                    "The path '" + rawPath + "' does not target a recognized attribute");
        }
    }

    /**
     * Determines whether a path targets a recognized schema URI or attribute. The path is first normalized
     * by stripping any filter expressions (e.g. {@code emails[type eq "work"].value} becomes {@code emails.value}),
     * then checked against recognized schema URIs and attribute paths.
     *
     * <p>A sub-attribute path such as {@code emails.type} or {@code name.givenName} is only recognized when the
     * sub-attribute actually exists: either it is individually declared (e.g. {@code name.givenName}) or it is a
     * canonical member of the complex attribute's backing type (e.g. {@code emails.type}). An unknown descendant
     * such as {@code emails.bogus} or {@code name.bogus} is not recognized (rather than accepted by a loose
     * prefix match).
     */
    private boolean isRecognizedPath(String rawPath) {
        String normalizedPath = normalizePath(rawPath);

        Set<String> recognized = getRecognizedSchemaUris();
        if (recognized.contains(normalizedPath)) {
            return true;
        }

        for (ModelSchema<M, R> s : schemas) {
            // resolves simple attributes, individually declared sub-attributes (e.g. name.givenName), the
            // synthesized "<attr>.value" of a multi-valued complex attribute, and all extension path forms
            if (s.getAttributeByPath(normalizedPath) != null) {
                return true;
            }
            for (Attribute<M, R> attr : s.getAttributes().values()) {
                String attrName = attr.getName();
                String parentName = attr.getParentName();

                // exact match on an attribute name, or on the bare parent of a declared complex attribute
                // whose sub-attributes are individually registered (e.g. the bare "name" of "name.givenName")
                if (normalizedPath.equalsIgnoreCase(attrName)
                        || (parentName != null && normalizedPath.equalsIgnoreCase(parentName))) {
                    return true;
                }

                // a canonical sub-attribute of a complex attribute that is not individually declared, such as
                // "emails.type"/"emails.primary" for the multi-valued "emails" (backed by Email). Only genuine
                // canonical members are accepted; unknown descendants are not recognized.
                Class<?> complexType = attr.getComplexType();
                if (complexType != null) {
                    for (String sub : getComplexSubAttributes(complexType)) {
                        if (normalizedPath.equalsIgnoreCase(attrName + "." + sub)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns the canonical sub-attribute names of a complex attribute's backing Java type (e.g. for the SCIM
     * {@code emails} attribute, backed by {@code Email}, this yields {@code value}, {@code display}, {@code type},
     * {@code primary} and {@code $ref}). Names are derived via Jackson bean introspection so that {@code @JsonProperty}
     * overrides (such as {@code $ref}) are honored, and cached per type since the mapping is static.
     */
    private static Set<String> getComplexSubAttributes(Class<?> complexType) {
        return COMPLEX_SUB_ATTRIBUTES.computeIfAbsent(complexType, type -> {
            Set<String> subs = new HashSet<>();
            BeanDescription description = JsonSerialization.mapper.getSerializationConfig()
                    .introspect(JsonSerialization.mapper.constructType(type));
            for (BeanPropertyDefinition property : description.findProperties()) {
                subs.add(property.getName());
            }
            return subs;
        });
    }

    /**
     * Validates a pathless PATCH operation whose {@code value} is a JSON object. Per RFC 7644 section 3.5.2,
     * {@code add} and {@code replace} operations may omit {@code path} and instead carry the attributes to
     * modify as members of the {@code value} object. Each top-level member is treated as an attribute path
     * (or extension schema URI) and validated the same way as an explicit operation path. When a member is a
     * recognized extension schema URI whose value is itself an object, its nested members are validated as
     * schema-qualified attributes of that URI (e.g. {@code {"urn:...:enterprise:2.0:User": {"bogus": "x"}}} is
     * rejected because {@code urn:...:enterprise:2.0:User:bogus} is not a recognized attribute). Throws
     * {@link ScimNoTargetException} for any member that does not target a recognized attribute.
     */
    private void validatePatchValue(JsonNode value) {
        Set<String> recognizedUris = getRecognizedSchemaUris();
        for (Map.Entry<String, JsonNode> member : value.properties()) {
            String name = member.getKey();
            JsonNode memberValue = member.getValue();

            if (recognizedUris.contains(name) && memberValue != null && memberValue.isObject()) {
                validateExtensionMembers(name, memberValue);
            } else {
                validatePatchPath(name);
            }
        }
    }

    /**
     * Validates the members of a value object nested under a recognized extension schema URI. Each nested member
     * is checked as the schema-qualified attribute {@code schemaUri:memberName}, throwing
     * {@link ScimNoTargetException} for any member that does not target a recognized attribute of that schema.
     */
    private void validateExtensionMembers(String schemaUri, JsonNode object) {
        for (Iterator<String> names = object.fieldNames(); names.hasNext(); ) {
            String qualified = schemaUri + ":" + names.next();
            if (!isRecognizedPath(qualified)) {
                throw new ScimNoTargetException(
                        "The path '" + qualified + "' does not target a recognized attribute");
            }
        }
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
