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
 * @param <R> the representation type, must extend {@link BaseClientRepresentation}
 */
public abstract class BaseClientModelSchema<R extends BaseClientRepresentation>
        implements ModelSchema<ClientModel, R> {

    public static final Set<String> QUERYABLE_FIELDS = Set.of(
            "clientId", "enabled", "description", "displayName",
            "protocol", "appUrl", "createdTimestamp", "updatedTimestamp");

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
        map.put("redirectUris",     multivaluedStringAttr("redirectUris",    (rep, v) -> rep.setRedirectUris(v)));
        map.put("roles",            multivaluedStringAttr("roles",           (rep, v) -> rep.setRoles(v)));
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
                .modelAttributeResolver(a -> entityField)
                .withModelSetter(
                        modelSetter != null ? (TriConsumer<ClientModel, String, String>) (model, n, v) -> modelSetter.accept(model, v) : null,
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
                        modelSetter != null ? (TriConsumer<ClientModel, String, Boolean>) (model, n, v) -> modelSetter.accept(model, v) : null,
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
                        modelSetter != null ? (TriConsumer<ClientModel, String, Long>) (model, n, v) -> modelSetter.accept(model, v) : null,
                        (BiConsumer<R, Long>) (rep, v) -> repSetter.accept(rep, v))
                .build()
                .get(0);
    }

    /**
     * Builds a multivalued string attribute whose value is retrieved from the model by
     * {@link #getAttributeValue} (keyed on the schema name) and written to the representation
     * via {@code repSetter}.
     */
    protected Attribute<ClientModel, R> multivaluedStringAttr(String name,
            BiConsumer<R, Set<String>> repSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .modelAttributeResolver(a -> name)
                .multivalued()
                .withModelSetter(
                        null,
                        repSetter)
                .build()
                .get(0);
    }

    /**
     * Builds a boolean attribute whose entity-field key equals the schema name (used for
     * protocol-specific boolean attributes where the model getter is handled in
     * {@link #getAttributeValue}).
     */
    protected Attribute<ClientModel, R> protocolBoolAttr(String name,
            BiConsumer<R, Boolean> repSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .modelAttributeResolver(a -> name)
                .bool()
                .withModelSetter(
                        null,
                        repSetter)
                .build()
                .get(0);
    }

    /**
     * Builds a string attribute whose entity-field key equals the schema name (used for
     * protocol-specific string attributes where the model getter is handled in
     * {@link #getAttributeValue}).
     */
    protected Attribute<ClientModel, R> protocolStringAttr(String name,
            BiConsumer<R, String> repSetter) {
        return Attribute.<ClientModel, R>simple(name)
                .modelAttributeResolver(a -> name)
                .withModelSetter(
                        null,
                        repSetter)
                .build()
                .get(0);
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
     * Mirrors {@code AbstractModelSchema.populateResourceType} but without {@code setId}/{@code addSchema} calls.
     */
    @Override
    public void populate(R representation, ClientModel model, List<String> attributes, List<String> excludedAttributes) {
        for (Attribute<ClientModel, R> attribute : this.attributes.values()) {
            if (attribute.isExcluded(this, attributes, excludedAttributes)) {
                continue;
            }
            Object value = getAttributeValue(model, attribute.getModelAttributeName());
            attribute.set(representation, value);
        }
    }

    /**
     * Returns the value of the named model attribute (using the <em>entity-column / schema</em> name).
     * Subclasses should override to handle protocol-specific attribute names, calling
     * {@code super.getAttributeValue} for the base fields.
     *
     * TODO: should be on the Attribute class
     */
    protected Object getAttributeValue(ClientModel model, String name) {
        return switch (name) {
            case "protocol"              -> model.getProtocol();
            case "id"                    -> model.getId();
            case "clientId"              -> model.getClientId();
            case "enabled"               -> model.isEnabled();
            case "description"           -> model.getDescription();
            case "name"                  -> model.getName();
            case "baseUrl"               -> model.getBaseUrl();
            case "redirectUris"          -> new LinkedHashSet<>(model.getRedirectUris());
            case "roles"                 -> model.getRolesStream().map(RoleModel::getName).collect(Collectors.toCollection(LinkedHashSet::new));
            case "createdTimestamp"      -> model.getCreatedTimestamp();
            case "lastModifiedTimestamp" -> model.getLastModifiedTimestamp();
            default                      -> null;
        };
    }

    /** Factory method — subclasses return a fresh, empty representation instance. */
    public abstract R createRepresentation();

    // ---- Methods not needed for query/projection use ----

    /**
     * Populates {@code model} from {@code representation} by calling the model-setter side of each attribute.
     * Read-only attributes (createdTimestamp, updatedTimestamp) are silently skipped.
     */
    @Override
    public void populate(ClientModel model, R representation) {
        throw new UnsupportedOperationException("populate(ClientModel, R) not yet implemented");
    }

    @Override
    public void populate(R representation, ClientModel model) {
        throw new UnsupportedOperationException("populate(R, ClientModel) is not supported — use populate(R, ClientModel, List, List) instead");
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
