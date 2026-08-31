package org.keycloak.services.client.scim;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.keycloak.common.util.TriConsumer;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;

/**
 * Abstract schema for client models.
 * 
 * TODO: determine if common base logic with AbstractModelSchema should be captured with a parent class
 *
 * @param <R> the representation type, must extend {@link BaseClientRepresentation}
 */
public abstract class BaseClientModelSchema<R extends BaseClientRepresentation>
        implements ModelSchema<ClientModel, R> {

    // TODO: should be metadata on the Attribute
    public static final Set<String> QUERYABLE_FIELDS = Set.of(
            "roles", // TODO: see ClientQueryTest, only a single predicate is currently correctly supported
            "clientId", "enabled", "description", "displayName",
            "protocol", "appUrl", "createdTimestamp", "updatedTimestamp",
            "auth.method");

    private final Map<String, Attribute<ClientModel, R>> attributes;

    protected BaseClientModelSchema() {
        Map<String, Attribute<ClientModel, R>> map = new LinkedHashMap<>();
        map.put("protocol",         stringAttr("protocol",         "protocol",              BaseClientRepresentation::setProtocol,       ClientModel::setProtocol));
        map.put("uuid",             stringAttr("uuid",             "id",                    BaseClientRepresentation::setUuid,           null));  // read-only
        map.put("clientId",         stringAttr("clientId",         "clientId",              BaseClientRepresentation::setClientId,       ClientModel::setClientId));
        map.put("enabled",          boolAttr  ("enabled",          "enabled",               BaseClientRepresentation::setEnabled,        (model, v) -> model.setEnabled(Boolean.TRUE.equals(v))));
        map.put("description",      stringAttr("description",      "description",           BaseClientRepresentation::setDescription,    ClientModel::setDescription));
        map.put("displayName",      stringAttr("displayName",      "name",                  BaseClientRepresentation::setDisplayName,    ClientModel::setName));
        map.put("appUrl",           stringAttr("appUrl",           "baseUrl",               BaseClientRepresentation::setAppUrl,         ClientModel::setBaseUrl));
        map.put("redirectUris",     multivaluedStringAttr("redirectUris",    "redirectUris", BaseClientRepresentation::setRedirectUris, (BiConsumer<ClientModel, Set<String>>) (model, uris) -> model.setRedirectUris(uris != null ? new LinkedHashSet<>(uris) : null)));
        map.put("roles",            multivaluedStringAttr("roles",           "roles",       BaseClientRepresentation::setRoles, null));
        map.put("createdTimestamp", longAttr  ("createdTimestamp", "createdTimestamp",      BaseClientRepresentation::setCreatedTimestamp,  null));  // read-only
        map.put("updatedTimestamp", longAttr  ("updatedTimestamp", "lastModifiedTimestamp", BaseClientRepresentation::setUpdatedTimestamp, null));  // read-only
        addProtocolAttributes(map);
        this.attributes = Map.copyOf(map);
    }

    /**
     * Hook for subclasses to register protocol-specific attributes into the shared attribute map
     * before it is sealed. Called at the end of the base constructor.
     */
    protected void addProtocolAttributes(Map<String, Attribute<ClientModel, R>> map) {
        // default: no additional attributes
    }

    @SuppressWarnings("unchecked")
    private Attribute<ClientModel, R> stringAttr(String name, String entityField,
            BiConsumer<BaseClientRepresentation, String> repSetter,
            BiConsumer<ClientModel, String> modelSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .caseExact()
                .modelAttributeResolver(a -> entityField)
                .withModelSetter(
                        modelSetter != null ? (TriConsumer<ClientModel, String, Object>) (model, n, v) -> modelSetter.accept(model, (String) v) : null,
                        (BiConsumer<R, String>) (rep, v) -> repSetter.accept(rep, v))
                .build()
                .get(0);
    }

    @SuppressWarnings("unchecked")
    private Attribute<ClientModel, R> boolAttr(String name, String entityField,
            BiConsumer<BaseClientRepresentation, Boolean> repSetter,
            BiConsumer<ClientModel, Boolean> modelSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .modelAttributeResolver(a -> entityField)
                .bool()
                .withModelSetter(
                        modelSetter != null ? (TriConsumer<ClientModel, String, Object>) (model, n, v) -> modelSetter.accept(model, (Boolean) v) : null,
                        (BiConsumer<R, Boolean>) (rep, v) -> repSetter.accept(rep, v))
                .build()
                .get(0);
    }

    @SuppressWarnings("unchecked")
    private Attribute<ClientModel, R> longAttr(String name, String entityField,
            BiConsumer<BaseClientRepresentation, Long> repSetter,
            BiConsumer<ClientModel, Long> modelSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .modelAttributeResolver(a -> entityField)
                .timestamp()
                .withModelSetter(
                        modelSetter != null ? (TriConsumer<ClientModel, String, Object>) (model, n, v) -> modelSetter.accept(model, (Long) v) : null,
                        (BiConsumer<R, Long>) (rep, v) -> repSetter.accept(rep, v))
                .build()
                .get(0);
    }

    @SuppressWarnings("unchecked")
    protected <V> Attribute<ClientModel, R> multivaluedStringAttr(String name,
            String entityField,
            BiConsumer<R, Set<String>> repSetter,
            BiConsumer<ClientModel, Set<V>> modelSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .multivalued()
                .modelAttributeResolver(a -> entityField)
                .withModelSetter(
                        modelSetter != null ? (TriConsumer<ClientModel, String, Object>) (model, n, v) -> modelSetter.accept(model, (Set<V>) v) : null,
                        repSetter)
                .build().get(0);
    }

    @SuppressWarnings("unchecked")
    protected Attribute<ClientModel, R> protocolBoolAttr(String name,
            String entityField,
            BiConsumer<R, Boolean> repSetter,
            BiConsumer<ClientModel, Boolean> modelSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .bool()
                .modelAttributeResolver(a -> entityField)
                .withModelSetter(
                        modelSetter != null ? (TriConsumer<ClientModel, String, Object>) (model, n, v) -> modelSetter.accept(model, (Boolean) v) : null,
                        repSetter)
                .build().get(0);
    }

    @SuppressWarnings("unchecked")
    protected <V> Attribute<ClientModel, R> protocolStringAttr(String name,
            String entityField,
            BiConsumer<R, String> repSetter,
            BiConsumer<ClientModel, V> modelSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .modelAttributeResolver(a -> entityField)
                .withModelSetter(
                        modelSetter != null ? (TriConsumer<ClientModel, String, Object>) (model, n, v) -> modelSetter.accept(model, (V) v) : null,
                        repSetter)
                .build().get(0);
    }

    @SuppressWarnings("unchecked")
    protected <V> Attribute<ClientModel, R> customAttr(String name,
            String entityField,
            BiConsumer<R, V> repSetter,
            BiConsumer<ClientModel, V> modelSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .modelAttributeResolver(a -> entityField)
                .withModelSetter(
                        modelSetter != null ? (TriConsumer<ClientModel, String, Object>) (model, n, v) -> modelSetter.accept(model, (V) v) : null,
                        repSetter)
                .build().get(0);
    }

    @Override
    public Map<String, Attribute<ClientModel, R>> getAttributes() {
        return attributes;
    }

    @Override
    public Attribute<ClientModel, R> getAttributeByPath(String path) {
        return attributes.get(path);
    }

    /**
     * Populates {@code representation} with fields from {@code model}, honouring inclusion/exclusion filters.
     * Mirrors {@code AbstractModelSchema.populateResourceType} but without {@code setId}/{@code addSchema} calls
     * 
     * NOTE/TODO: the name passed to {@link #getAttributeValue(ClientModel, String)} is the representation attribute name,
     * not the model attribute name as is done in the other scim logic. We do not always have a 1-1 mapping to a model field
     */
    @Override
    public void populate(R representation, ClientModel model, List<String> attributes, List<String> excludedAttributes) {
        for (Attribute<ClientModel, R> attribute : this.attributes.values()) {
            if (attribute.isExcluded(this, attributes, excludedAttributes)) {
                continue;
            }
            Object value = getAttributeValue(model, attribute.getName());
            attribute.set(representation, value);
        }
    }

    /**
     * Returns the value of the named attribute
     * Subclasses should override to handle protocol-specific attribute names, calling
     * {@code super.getAttributeValue} for the base fields.
     *
     * TODO: should be on the Attribute class
     */
    protected Object getAttributeValue(ClientModel model, String name) {
        return switch (name) {
            case "protocol"              -> model.getProtocol();
            case "uuid"                  -> model.getId();
            case "clientId"              -> model.getClientId();
            case "enabled"               -> model.isEnabled();
            case "description"           -> model.getDescription();
            case "displayName"           -> model.getName();
            case "appUrl"                -> model.getBaseUrl();
            case "redirectUris"          -> new LinkedHashSet<>(model.getRedirectUris());
            case "roles"                 -> model.getRolesStream().map(RoleModel::getName).collect(Collectors.toCollection(LinkedHashSet::new));
            case "createdTimestamp"      -> model.getCreatedTimestamp();
            case "updatedTimestamp"      -> model.getLastModifiedTimestamp();
            default                      -> null;
        };
    }

    /** Factory method — subclasses return a fresh, empty representation instance. */
    public abstract R createRepresentation();

    // TODO: should be tracked by Attribute.isImmutable
    public Set<String> getWritableFields() {
        return getAttributes().keySet().stream()
                .filter(name -> !Set.of("uuid", "createdTimestamp", "updatedTimestamp").contains(name))
                .collect(Collectors.toUnmodifiableSet());
    }

    public R fromModel(ClientModel model) {
        return fromModel(model, (List<String>) null);
    }

    public R fromModel(ClientModel model, boolean includeReadOnlyFields) {
        return fromModel(model, includeReadOnlyFields ? null : getWritableFields());
    }

    public R fromModel(ClientModel model, Set<String> includeFields) {
        List<String> attributes = includeFields != null && !includeFields.isEmpty() ? List.copyOf(includeFields) : null;
        return fromModel(model, attributes);
    }

    public R fromModel(ClientModel model, List<String> attributes) {
        R rep = createRepresentation();
        populate(rep, model, attributes, null);
        return rep;
    }

    /**
     * TODO: should be on the Attribute class
     */
    public Object getRepresentationValue(R rep, String name) {
        return switch (name) {
            case "protocol"         -> rep.getProtocol();
            case "uuid"             -> rep.getUuid();
            case "clientId"         -> rep.getClientId();
            case "enabled"          -> rep.getEnabled();
            case "description"      -> rep.getDescription();
            case "displayName"      -> rep.getDisplayName();
            case "appUrl"           -> rep.getAppUrl();
            case "redirectUris"     -> rep.getRedirectUris();
            case "roles"            -> rep.getRoles();
            case "createdTimestamp" -> rep.getCreatedTimestamp();
            case "updatedTimestamp" -> rep.getUpdatedTimestamp();
            default                 -> null;
        };
    }

    public void applyProjection(R rep, Set<String> includeFields) {
        if (includeFields == null || includeFields.isEmpty()) return;
        for (Attribute<ClientModel, R> attribute : getAttributes().values()) {
            if (!includeFields.contains(attribute.getName())) {
                attribute.set(rep, null);
            }
        }
    }

    /**
     * Populates {@code model} from {@code representation} by iterating over all attributes,
     * getting their representation value, and calling the attribute's model setter.
     * Read-only attributes (createdTimestamp, updatedTimestamp, uuid, roles) have a null model setter and are skipped.
     */
    @Override
    public void populate(ClientModel model, R representation) {
        for (Attribute<ClientModel, R> attribute : getAttributes().values()) {
            Object value = getRepresentationValue(representation, attribute.getName());
            attribute.setModelAttribute(model, value);
        }
    }

    @Override
    public void populate(R representation, ClientModel model) {
        populate(representation, model, null, null);
    }

    @Override
    public void validate(R representation) throws ModelValidationException {
        throw new UnsupportedOperationException("validate is not supported");
    }

    @Override
    public String getId() {
        return ""; // anonymous
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("not needed for v2");
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("not needed for v2");
    }
}
