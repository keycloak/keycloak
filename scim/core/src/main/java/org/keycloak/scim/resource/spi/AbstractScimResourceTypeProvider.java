package org.keycloak.scim.resource.spi;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterException;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.filter.ScimFilterParserBaseVisitor;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.PatchRequest.PatchOperation;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.group.Member;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.Email;
import org.keycloak.scim.resource.user.GroupMembership;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.antlr.v4.runtime.tree.ParseTree;

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

    /**
     * Sub-attributes that a backing Java type inherits (from {@link org.keycloak.scim.resource.common.MultiValuedAttribute})
     * but that are not part of the SCIM definition of the attributes it backs, and therefore must not be accepted.
     * For example, {@code Member} (SCIM Group {@code members}) and {@code GroupMembership} (SCIM User {@code groups})
     * are reference-style multi-valued attributes that do not define a {@code primary} sub-attribute, even though the
     * shared bean declares one; conversely {@code Email} (SCIM User {@code emails}) does not define a {@code $ref}
     * sub-attribute, even though it inherits one.
     */
    private static final Map<Class<?>, Set<String>> NON_SCHEMA_SUB_ATTRIBUTES = Map.of(
            Member.class, Set.of("primary"),
            GroupMembership.class, Set.of("primary"),
            Email.class, Set.of("$ref"));

    /**
     * Common SCIM resource attributes ({@code id}, {@code schemas}, {@code meta}, {@code meta.resourceType},
     * {@code meta.location}, {@code meta.version}) that are not backed by a {@link ModelSchema} {@link Attribute} - they are populated
     * directly on the resource representation or computed by the service layer rather than mapped from the model.
     * They are nonetheless recognized attribute paths so a PATCH targeting one of them is treated as a no-op
     * (read-only attributes are ignored, not rejected) instead of failing with {@code noTarget}.
     */
    private static final Set<String> COMMON_READ_ONLY_PATHS = Set.of(
            "id", "schemas", "meta", "meta.resourcetype", "meta.location", "meta.version");

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

        // computed once per request rather than per operation/field, as the recognized set is invariant
        Set<String> recognized = getRecognizedSchemaUris();

        for (PatchOperation operation : operations) {
            String op = operation.getOp();

            if (isBlank(op)) {
                throw new ModelValidationException("Missing operation for patch operation");
            }

            String path = operation.getPath();
            JsonNode value = operation.getValue();

            if (isBlank(path)) {
                // a blank (whitespace-only or empty) path is not a valid attribute path; treat it the same as an
                // absent one so validation and application agree on this being a pathless operation
                path = null;
            }

            if (path != null) {
                if (value == null && !"remove".equalsIgnoreCase(op)) {
                    // per RFC 7644 section 3.5.2, "add" and "replace" require a value; without this check a
                    // missing/null value reaches the schema layer's own null check as an uncaught NPE (HTTP 500)
                    // rather than a clean 400 response
                    throw new ModelValidationException(
                            "A '" + op + "' operation with an explicit path requires a value");
                }
                validateFilterAttributes(path, recognized);
                validatePatchMember(path, value, recognized);
            } else if (value != null && value.isObject() && !value.isEmpty()) {
                validatePatchValue(value, recognized);
            } else {
                // per RFC 7644 section 3.5.2, a pathless add/replace must carry the attributes to modify as
                // members of the value object; a scalar, null, array or empty value provides no target
                throw new ModelValidationException("A pathless patch operation requires a non-empty object value");
            }

            for (ModelSchema<M, R> schema : schemas) {
                // core attributes are stored unqualified; a fully-qualified core path must be stripped before
                // being applied, mirroring the stripping already done for recognition in isRecognizedPath
                String schemaPath = stripCorePrefix(schema, path);

                switch (op.toLowerCase(Locale.ROOT)) {
                    case "add" -> schema.add(model, schemaPath, value);
                    case "replace" -> schema.replace(existing, model, schemaPath, value);
                    case "remove" -> schema.remove(existing, model, schemaPath);
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
     * Validates that all schema URIs in the request's {@code schemas} array and every attribute carried by the
     * {@code extensions} map (including nested sub-attributes of complex extension attributes) are recognized by
     * at least one registered schema. Throws {@link ScimInvalidValueException} (HTTP 400,
     * {@code scimType=invalidValue}) if any are not.
     *
     * <p>Called from {@link #populate(Model, ResourceTypeRepresentation)} to validate POST and PUT requests.
     */
    private void validateSchemas(R resource) {
        Set<String> recognized = getRecognizedSchemaUris();

        Set<String> requestSchemas = resource.getSubmittedSchemas();
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
                            "Unrecognized schema URI '" + key + "'");
                }

                // the schema URI is recognized; validate the attributes it carries, recursing into complex values,
                // so that e.g. {"urn:custom:schema:User": {"bogus": "x"}} or an undeclared descendant of a complex
                // attribute is rejected rather than silently ignored
                JsonNode extension = JsonSerialization.mapper.valueToTree(entry.getValue());
                if (extension.isObject()) {
                    for (Map.Entry<String, JsonNode> member : extension.properties()) {
                        validateExtensionMember(key + ":" + member.getKey(), member.getValue(), recognized, false);
                    }
                } else {
                    // a schema URI is a namespace that must carry an object of attributes; a scalar or array value
                    // (e.g. {"urn:custom:schema:User": "x"}) targets no attribute and is rejected
                    throw new ScimInvalidValueException(
                            "Schema '" + key + "' must be a JSON object of attributes");
                }
            }
        }
    }

    /**
     * Validates a single PATCH member, i.e. an explicit operation {@code path}/{@code value} pair or a member of a
     * pathless {@code value} object. When {@code name} is a recognized schema URI carrying a JSON object, its members
     * are validated as schema-qualified attributes (e.g. {@code {"urn:...:User": {"bogus": "x"}}} is rejected because
     * {@code urn:...:User:bogus} is unknown). Otherwise {@code name} is treated as an attribute path and validated
     * together with the descendants of its {@code value}, so an object value carrying an unknown sub-attribute (e.g.
     * {@code path="name", value={"bogus":"x"}}) is rejected rather than silently ignored. Throws
     * {@link ScimNoTargetException} (HTTP 400, {@code scimType=noTarget}) for anything that does not target a
     * recognized attribute.
     */
    private void validatePatchMember(String name, JsonNode value, Set<String> recognized) {
        if (recognized.contains(name)) {
            // a recognized schema URI is a namespace, not an attribute: it must carry a non-empty object of
            // attributes. A scalar, array or empty object value (e.g. {"urn:...:User": "x"} or {"urn:...:User": {}})
            // targets no attribute and is rejected.
            if (value == null || !value.isObject() || value.isEmpty()) {
                throw new ScimNoTargetException(
                        "The path '" + name + "' is a schema URI and must carry a non-empty object of attributes");
            }
            for (Map.Entry<String, JsonNode> nested : value.properties()) {
                validateExtensionMember(name + ":" + nested.getKey(), nested.getValue(), recognized, true);
            }
        } else {
            validateExtensionMember(name, value, recognized, true);
        }
    }

    /**
     * Strips a fully-qualified core schema prefix (e.g. {@code urn:ietf:params:scim:schemas:core:2.0:User:}) from
     * {@code path} when {@code s} is the core schema and {@code path} starts with that prefix; {@code path} is
     * returned unchanged otherwise. Core attribute names are stored unqualified, so both attribute recognition
     * ({@link #isRecognizedPath}) and application ({@link ModelSchema#add}/{@code replace}/{@code remove}) must
     * resolve the same, unqualified form.
     */
    private static String stripCorePrefix(ModelSchema<?, ?> s, String path) {
        if (path == null || !s.isCore()) {
            return path;
        }
        String prefix = s.getId() + ":";
        if (path.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return path.substring(prefix.length());
        }
        return path;
    }

    /**
     * Determines whether a path targets a recognized schema URI or attribute. The path is first normalized
     * by stripping any filter expressions (e.g. {@code emails[type eq "work"].value} becomes {@code emails.value}),
     * then checked against recognized schema URIs and attribute paths.
     *
     * <p>Core attribute names are stored unqualified, so a fully-qualified core path such as
     * {@code urn:ietf:params:scim:schemas:core:2.0:User:nickName} has its (core) schema prefix stripped before the
     * attribute lookup - {@code getAttributeByPath} only strips the prefix for non-core schemas.
     *
     * <p>A sub-attribute path such as {@code emails.type} or {@code name.givenName} is only recognized when the
     * sub-attribute actually exists: either it is individually declared (e.g. {@code name.givenName}) or it is a
     * canonical member of the complex attribute's backing type (e.g. {@code emails.type}). An unknown descendant
     * such as {@code emails.bogus} or {@code name.bogus} is not recognized (rather than accepted by a loose
     * prefix match).
     */
    private boolean isRecognizedPath(String rawPath, Set<String> recognized) {
        String normalizedPath = normalizePath(rawPath);

        if (COMMON_READ_ONLY_PATHS.contains(normalizedPath.toLowerCase(Locale.ROOT))) {
            return true;
        }

        if (recognized.contains(normalizedPath)) {
            return true;
        }

        for (ModelSchema<M, R> s : schemas) {
            String localPath = stripCorePrefix(s, normalizedPath);

            // a common read-only path may also be submitted with the (optional) core schema prefix, e.g.
            // "urn:ietf:params:scim:schemas:core:2.0:User:id" - recheck against the stripped form too
            if (COMMON_READ_ONLY_PATHS.contains(localPath.toLowerCase(Locale.ROOT))) {
                return true;
            }

            // resolves simple attributes, individually declared sub-attributes (e.g. name.givenName), the
            // synthesized "<attr>.value" of a multi-valued complex attribute, and all extension path forms
            if (s.getAttributeByPath(localPath) != null) {
                return true;
            }
            for (Attribute<M, R> attr : s.getAttributes().values()) {
                String attrName = attr.getName();
                String parentName = attr.getParentName();

                // exact match on an attribute name, or on the bare parent of a declared complex attribute
                // whose sub-attributes are individually registered (e.g. the bare "name" of "name.givenName")
                if (localPath.equalsIgnoreCase(attrName)
                        || (parentName != null && localPath.equalsIgnoreCase(parentName))) {
                    return true;
                }

                // a canonical sub-attribute of a complex attribute that is not individually declared, such as
                // "emails.type"/"emails.primary" for the multi-valued "emails" (backed by Email). Only genuine
                // canonical members are accepted; unknown descendants are not recognized.
                Class<?> complexType = attr.getComplexType();
                if (complexType != null) {
                    for (String sub : getComplexSubAttributes(complexType)) {
                        if (localPath.equalsIgnoreCase(attrName + "." + sub)) {
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
     * overrides (such as {@code $ref}) are honored, minus any {@link #NON_SCHEMA_SUB_ATTRIBUTES inherited-but-undefined}
     * sub-attributes, and cached per type since the mapping is static.
     */
    private static Set<String> getComplexSubAttributes(Class<?> complexType) {
        return COMPLEX_SUB_ATTRIBUTES.computeIfAbsent(complexType, type -> {
            Set<String> subs = new HashSet<>();
            BeanDescription description = JsonSerialization.mapper.getSerializationConfig()
                    .introspect(JsonSerialization.mapper.constructType(type));
            for (BeanPropertyDefinition property : description.findProperties()) {
                subs.add(property.getName());
            }
            subs.removeAll(NON_SCHEMA_SUB_ATTRIBUTES.getOrDefault(type, Set.of()));
            return Set.copyOf(subs);
        });
    }

    /**
     * Validates a pathless PATCH operation whose {@code value} is a JSON object. Per RFC 7644 section 3.5.2,
     * {@code add} and {@code replace} operations may omit {@code path} and instead carry the attributes to
     * modify as members of the {@code value} object. Each top-level member is treated as an attribute path
     * (or extension schema URI) and validated the same way as an explicit operation path. When a member is a
     * recognized extension schema URI whose value is itself an object, its (possibly nested) members are validated
     * as schema-qualified attributes of that URI (e.g. {@code {"urn:...:User": {"bogus": "x"}}} is rejected because
     * {@code urn:...:User:bogus} is not a recognized attribute). Throws {@link ScimNoTargetException} for any member
     * that does not target a recognized attribute.
     */
    private void validatePatchValue(JsonNode value, Set<String> recognized) {
        for (Map.Entry<String, JsonNode> member : value.properties()) {
            validateFilterAttributes(member.getKey(), recognized);
            validatePatchMember(member.getKey(), member.getValue(), recognized);
        }
    }

    /**
     * Validates the attributes referenced inside a value-path filter of a PATCH path (for example the {@code type}
     * in {@code emails[type eq "work"].value}). Each such attribute must be a recognized sub-attribute of the
     * multi-valued attribute the filter is applied to; otherwise {@link #normalizePath(String) normalization} would
     * silently drop the filter, causing the operation to match nothing or - for {@code add}, which strips rather than
     * evaluates the filter - to mutate the whole attribute instead of the targeted element. Both an unrecognized
     * filter attribute and an unparseable filter expression propagate as {@link ScimFilterException} (HTTP 400,
     * {@code scimType=invalidFilter}), consistent with RFC 7644 Table 9, which classifies PATCH path filter
     * problems under {@code invalidFilter} rather than {@code noTarget}.
     *
     * <p>Only paths that actually contain a filter (a {@code '['}) are parsed, so this adds no overhead to
     * unfiltered PATCH operations or to any read/search request.
     *
     * <p>The filter's attributes are only checked once the filtered attribute itself ({@code parent}) is confirmed
     * to be recognized; otherwise this defers to the normal path-recognition check on the (filter-stripped) full
     * path, so that e.g. {@code bogus[type eq "work"].value} is reported as {@code noTarget} (the "bogus" attribute
     * does not exist at all) rather than {@code invalidFilter} (which would incorrectly suggest the filter itself,
     * as opposed to the attribute it is applied to, is the problem).
     */
    private void validateFilterAttributes(String rawPath, Set<String> recognized) {
        int filterStart = rawPath.indexOf('[');
        if (filterStart <= 0) {
            return;
        }
        int filterEnd = rawPath.lastIndexOf(']');
        if (filterEnd <= filterStart) {
            // a missing or out-of-order closing ']' is a malformed filter, not a "target does not exist" condition
            throw new ScimFilterException("Unbalanced filter delimiters in path '" + rawPath + "'");
        }

        String parent = rawPath.substring(0, filterStart);
        if (!isRecognizedPath(parent, recognized)) {
            return;
        }

        String filterExpression = rawPath.substring(filterStart + 1, filterEnd);

        // let a malformed filter surface as ScimFilterException (-> invalidFilter); it is not a noTarget condition
        ScimFilterParser.FilterContext filter = FilterUtils.parseFilter(filterExpression);

        validateFilterScope(parent, filter, recognized, rawPath);
    }

    /**
     * Validates the attributes referenced within a (possibly nested) value-path filter against the attribute path
     * they are actually scoped to. A comparison or presence test's attribute is validated relative to {@code scope};
     * a nested value-path (e.g. the {@code type} in {@code emails[type[value eq "x"]].value}) is itself validated
     * against {@code scope}, and its own filter is then recursively validated against {@code scope + "." + type},
     * not {@code scope} - so that a scalar sub-attribute used as a nested value-path (which cannot itself have
     * sub-attributes) is correctly rejected rather than accepted because its operand happens to match some other
     * sub-attribute of the outer attribute.
     */
    private void validateFilterScope(String scope, ParseTree tree, Set<String> recognized, String rawPath) {
        new ScimFilterParserBaseVisitor<Void>() {
            @Override
            public Void visitComparisonExpression(ScimFilterParser.ComparisonExpressionContext ctx) {
                checkAttribute(ctx.ATTRPATH().getText());
                return null;
            }

            @Override
            public Void visitPresentExpression(ScimFilterParser.PresentExpressionContext ctx) {
                checkAttribute(ctx.ATTRPATH().getText());
                return null;
            }

            @Override
            public Void visitValuePath(ScimFilterParser.ValuePathContext ctx) {
                String nestedAttribute = ctx.ATTRPATH().getText();
                checkAttribute(nestedAttribute);
                validateFilterScope(scope + "." + nestedAttribute, ctx.expression(), recognized, rawPath);
                return null;
            }

            private void checkAttribute(String attribute) {
                if (!isRecognizedPath(scope + "." + attribute, recognized)) {
                    // per RFC 7644 Table 9, a PATCH path filter problem is classified under invalidFilter, not
                    // noTarget - the latter is reserved for a well-formed filter that matches no records
                    throw new ScimFilterException("The filter attribute '" + attribute + "' in path '"
                            + rawPath + "' does not target a recognized attribute");
                }
            }
        }.visit(tree);
    }

    /**
     * Validates a single attribute carried under a recognized schema URI (whether from a POST/PUT extensions map or
     * a pathless PATCH value object), together with all of its descendants. The {@code path} is the schema-qualified
     * attribute path (e.g. {@code urn:custom:schema:User:assurance}); when {@code value} is a complex object or an
     * array of objects, its members are flattened into their dotted full paths (e.g. {@code ...:assurance.value}) and
     * only those flattened leaves are checked for recognition. Intermediate nodes are not required to be standalone
     * attributes because attribute resolution likewise resolves a nested value by its flattened dotted path
     * (see {@code AbstractModelSchema#resolveAttributes}), e.g. a value {@code {"assurance":{"level":"x"}}} declared as
     * {@code assurance.level} resolves directly - so an undeclared leaf such as {@code ...:assurance.bogus} is rejected
     * while a legitimate {@code ...:assurance.level} is accepted even though bare {@code ...:assurance} is not itself a
     * declared attribute. An empty object/array, or an array containing no object items, has no leaves to flatten;
     * {@code path} itself is then validated instead, so a bogus attribute name cannot be smuggled in merely by
     * giving it an empty or scalar-only value.
     *
     * @param patch when {@code true} an unrecognized leaf throws {@link ScimNoTargetException} (PATCH,
     *              {@code scimType=noTarget}); otherwise {@link ScimInvalidValueException} (POST/PUT,
     *              {@code scimType=invalidValue})
     */
    private void validateExtensionMember(String path, JsonNode value, Set<String> recognized, boolean patch) {
        // descend through complex values, accumulating the dotted path, and validate only the flattened leaves.
        // An empty object/array, or an array with no object items, has no leaves to descend into; fall through
        // to validate the accumulated path itself so a bogus attribute name is not silently let through merely
        // because the value it was given happens to be empty or an array of scalars.
        if (value != null && value.isObject()) {
            if (!value.isEmpty()) {
                for (Map.Entry<String, JsonNode> child : value.properties()) {
                    validateExtensionMember(path + "." + child.getKey(), child.getValue(), recognized, patch);
                }
                return;
            }
        } else if (value != null && value.isArray()) {
            boolean hasLeaf = false;
            for (JsonNode item : value) {
                if (item != null && item.isObject() && !item.isEmpty()) {
                    hasLeaf = true;
                    for (Map.Entry<String, JsonNode> child : item.properties()) {
                        validateExtensionMember(path + "." + child.getKey(), child.getValue(), recognized, patch);
                    }
                }
            }
            if (hasLeaf) {
                return;
            }
        }

        // scalar or null leaf, or an empty/scalar-only container with no leaves of its own: the accumulated
        // (flattened) path must target a recognized attribute
        if (!isRecognizedPath(path, recognized)) {
            if (patch) {
                throw new ScimNoTargetException(
                        "The path '" + path + "' does not target a recognized attribute");
            }
            throw new ScimInvalidValueException("Unrecognized attribute '" + path + "'");
        }
    }

    /**
     * Strips filter expressions from a PATCH path so that only the structural attribute path remains.
     * For example, {@code emails[type eq "work"].value} becomes {@code emails.value}. The character immediately
     * following the closing {@code ']'} must be a {@code '.'} or the end of the path; anything else (e.g. the
     * malformed {@code emails[type eq "work"]bogus}) is left untouched so it is rejected by the caller rather than
     * silently truncated to a valid attribute.
     */
    private String normalizePath(String rawPath) {
        int filterStart = rawPath.indexOf('[');
        if (filterStart > 0) {
            int filterEnd = rawPath.lastIndexOf(']');
            if (filterEnd == -1) {
                return rawPath;
            }
            String prefix = rawPath.substring(0, filterStart);
            if (filterEnd + 1 == rawPath.length()) {
                return prefix;
            }
            if (rawPath.charAt(filterEnd + 1) == '.') {
                return prefix + rawPath.substring(filterEnd + 1);
            }
            return rawPath;
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
