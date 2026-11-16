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

    // attribute names, which match the representation field names
    public static final String ATTR_PROTOCOL = "protocol";
    public static final String ATTR_UUID = "uuid";
    public static final String ATTR_CLIENT_ID = "clientId";
    public static final String ATTR_ENABLED = "enabled";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_DISPLAY_NAME = "displayName";
    public static final String ATTR_APP_URL = "appUrl";
    public static final String ATTR_REDIRECT_URIS = "redirectUris";
    public static final String ATTR_ROLES = "roles";
    public static final String ATTR_CREATED_TIMESTAMP = "createdTimestamp";
    public static final String ATTR_UPDATED_TIMESTAMP = "updatedTimestamp";
    // TODO: should be metadata on the Attribute
    public static final Set<String> QUERYABLE_FIELDS = Set.of(
            ATTR_ROLES, // TODO: see ClientQueryTest, only a single predicate is currently correctly supported
            ATTR_CLIENT_ID, ATTR_ENABLED, ATTR_DESCRIPTION, ATTR_DISPLAY_NAME,
            ATTR_PROTOCOL, ATTR_APP_URL, ATTR_CREATED_TIMESTAMP, ATTR_UPDATED_TIMESTAMP,
            OIDCClientModelSchema.ATTR_AUTH_METHOD);

    private final Map<String, Attribute<ClientModel, R>> attributes;

    /** Defaults for unset fields, in the form accepted by the attribute's representation setter. */
    private final Map<String, Object> defaults;

    protected BaseClientModelSchema() {
        Map<String, Attribute<ClientModel, R>> map = new LinkedHashMap<>();
        map.put(ATTR_PROTOCOL,          stringAttr(ATTR_PROTOCOL,          "protocol",              BaseClientRepresentation::setProtocol,          ClientModel::setProtocol));
        map.put(ATTR_UUID,              stringAttr(ATTR_UUID,              "id",                    BaseClientRepresentation::setUuid,              null));  // read-only
        map.put(ATTR_CLIENT_ID,         stringAttr(ATTR_CLIENT_ID,         "clientId",              BaseClientRepresentation::setClientId,          ClientModel::setClientId));
        map.put(ATTR_ENABLED,           boolAttr  (ATTR_ENABLED,           "enabled",               BaseClientRepresentation::setEnabled,           (model, v) -> model.setEnabled(Boolean.TRUE.equals(v))));
        map.put(ATTR_DESCRIPTION,       stringAttr(ATTR_DESCRIPTION,       "description",           BaseClientRepresentation::setDescription,       ClientModel::setDescription));
        map.put(ATTR_DISPLAY_NAME,      stringAttr(ATTR_DISPLAY_NAME,      "name",                  BaseClientRepresentation::setDisplayName,       ClientModel::setName));
        map.put(ATTR_APP_URL,           stringAttr(ATTR_APP_URL,           "baseUrl",               BaseClientRepresentation::setAppUrl,            ClientModel::setBaseUrl));
        map.put(ATTR_REDIRECT_URIS,     multivaluedStringAttr(ATTR_REDIRECT_URIS,    "redirectUris", BaseClientRepresentation::setRedirectUris, (BiConsumer<ClientModel, Set<String>>) (model, uris) -> model.setRedirectUris(uris != null ? new LinkedHashSet<>(uris) : null)));
        map.put(ATTR_ROLES,             multivaluedStringAttr(ATTR_ROLES,            "roles",        BaseClientRepresentation::setRoles, null));
        map.put(ATTR_CREATED_TIMESTAMP, longAttr  (ATTR_CREATED_TIMESTAMP, "createdTimestamp",      BaseClientRepresentation::setCreatedTimestamp,  null));  // read-only
        map.put(ATTR_UPDATED_TIMESTAMP, longAttr  (ATTR_UPDATED_TIMESTAMP, "lastModifiedTimestamp", BaseClientRepresentation::setUpdatedTimestamp,  null));  // read-only
        addProtocolAttributes(map);
        this.attributes = Map.copyOf(map);
        Map<String, Object> defaultValues = new LinkedHashMap<>();
        defaultValues.put(ATTR_ENABLED, Boolean.valueOf(BaseClientRepresentation.DEFAULT_ENABLED));
        addProtocolDefaults(defaultValues);
        this.defaults = Map.copyOf(defaultValues);
    }

    /**
     * Hook for subclasses to register protocol-specific attributes into the shared attribute map
     * before it is sealed. Called at the end of the base constructor.
     */
    protected void addProtocolAttributes(Map<String, Attribute<ClientModel, R>> map) {
        // default: no additional attributes
    }

    /**
     * Hook for subclasses to register protocol-specific defaults, keyed by attribute name, before the map is sealed.
     * Values are in the form accepted by the attribute's representation setter.
     * The defaults should match the {@code @Schema(defaultValue)} documented on the representation.
     */
    protected void addProtocolDefaults(Map<String, Object> defaults) {
        // default: no additional defaults
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
            case ATTR_PROTOCOL               -> model.getProtocol();
            case ATTR_UUID                   -> model.getId();
            case ATTR_CLIENT_ID              -> model.getClientId();
            case ATTR_ENABLED                -> model.isEnabled();
            case ATTR_DESCRIPTION            -> model.getDescription();
            case ATTR_DISPLAY_NAME           -> model.getName();
            case ATTR_APP_URL                -> model.getBaseUrl();
            case ATTR_REDIRECT_URIS          -> new LinkedHashSet<>(model.getRedirectUris());
            case ATTR_ROLES                  -> model.getRolesStream().map(RoleModel::getName).collect(Collectors.toCollection(LinkedHashSet::new));
            case ATTR_CREATED_TIMESTAMP      -> model.getCreatedTimestamp();
            case ATTR_UPDATED_TIMESTAMP      -> model.getLastModifiedTimestamp();
            default                          -> null;
        };
    }

    /** Factory method — subclasses return a fresh, empty representation instance. */
    public abstract R createRepresentation();

    // TODO: should be tracked by Attribute.isImmutable
    public Set<String> getWritableFields() {
        return getAttributes().keySet().stream()
                .filter(name -> !Set.of(ATTR_UUID, ATTR_CREATED_TIMESTAMP, ATTR_UPDATED_TIMESTAMP).contains(name))
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
            case ATTR_PROTOCOL          -> rep.getProtocol();
            case ATTR_UUID              -> rep.getUuid();
            case ATTR_CLIENT_ID         -> rep.getClientId();
            case ATTR_ENABLED           -> rep.getEnabled();
            case ATTR_DESCRIPTION       -> rep.getDescription();
            case ATTR_DISPLAY_NAME      -> rep.getDisplayName();
            case ATTR_APP_URL           -> rep.getAppUrl();
            case ATTR_REDIRECT_URIS     -> rep.getRedirectUris();
            case ATTR_ROLES             -> rep.getRoles();
            case ATTR_CREATED_TIMESTAMP -> rep.getCreatedTimestamp();
            case ATTR_UPDATED_TIMESTAMP -> rep.getUpdatedTimestamp();
            default                     -> null;
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

    /**
     * Applies the schema defaults to the unset fields of the representation,
     * prior to it being mapped to a model.
     * Subclasses should call {@code super.applyDefaults} and may add defaults that are conditional.
     *
     * @param existing the current model, or null when the client is being created
     * @param patch true if the representation is the current state merged with a patch
     */
    public void applyDefaults(R representation, ClientModel existing, boolean patch) {
        defaults.forEach((name, value) -> {
            if (getRepresentationValue(representation, name) == null) {
                attributes.get(name).set(representation, value);
            }
        });
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
