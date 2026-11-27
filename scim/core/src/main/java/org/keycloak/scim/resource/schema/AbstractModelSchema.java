package org.keycloak.scim.resource.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.keycloak.models.Model;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.common.MultiValuedAttribute;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.schema.attribute.ScimDateTimeUtil;
import org.keycloak.scim.resource.schema.path.Path;
import org.keycloak.scim.resource.spi.ScimMutabilityException;
import org.keycloak.scim.resource.spi.ScimNoTargetException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import static java.util.Optional.ofNullable;

import static org.keycloak.scim.resource.schema.AbstractModelSchema.Operation.ADD;
import static org.keycloak.scim.resource.schema.AbstractModelSchema.Operation.REMOVE;
import static org.keycloak.scim.resource.schema.AbstractModelSchema.Operation.SET;
import static org.keycloak.utils.JsonUtils.getJsonValue;
import static org.keycloak.utils.StringUtil.isBlank;

public abstract class AbstractModelSchema<M extends Model, R extends ResourceTypeRepresentation> implements ModelSchema<M, R> {

    enum Operation {
        SET, ADD, REMOVE
    }

    private final String id;
    private Map<String, Attribute<M, R>> attributes;

    protected AbstractModelSchema(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Attribute<M, R>> getAttributes() {
        if (attributes == null) {
            attributes = getAttributeMappers();
        }
        return attributes;
    }

    @Override
    public void populate(M model, R representation) {
        validate(representation);
        populateModel(model, representation);
        representation.setId(model.getId());
    }

    @Override
    public void populate(R resource, M model) {
        populateResourceType(resource, model, null, null);
        resource.setId(model.getId());
    }

    @Override
    public void populate(R resource, M model, List<String> requestedAttributes, List<String> excludedAttributes) {
        populateResourceType(resource, model, requestedAttributes, excludedAttributes);
        resource.setId(model.getId());
    }

    @Override
    public void validate(R representation) throws ModelValidationException {
        // validate here the schema
    }

    @Override
    public void add(M model, String rawPath, JsonNode value) {
        Objects.requireNonNull(model, "model cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        String path = new Path(this, rawPath).getPath();

        for (Entry<Attribute<M, R>, JsonNode> entry : resolveAttributes(path, value).entrySet()) {
            setValue(model, entry.getKey(), entry.getValue(), ADD);
        }
    }

    @Override
    public void remove(R resource, M model, String rawPath) {
        remove(resource, model, rawPath, null);
    }

    @Override
    public void remove(R resource, M model, String rawPath, JsonNode value) {
        Objects.requireNonNull(model, "model cannot be null");

        if (isBlank(rawPath)) {
            throw new ModelValidationException("Missing path for patch operation remove");
        }

        Path path = new Path(this, rawPath);
        boolean hasValue = value != null && !value.isNull();

        if (hasValue && path.hasFilter()) {
            // the two would select the values to remove in different ways, and one of them would be silently ignored
            throw new ModelValidationException("Patch operation remove cannot have both a filter in the path and a value");
        }

        for (Entry<Attribute<M, R>, JsonNode> entry : resolveAttributes(path.getPath(), NullNode.getInstance()).entrySet()) {
            Attribute<M, R> attribute = entry.getKey();
            JsonNode values;

            if (selectsByCurrentEntries(path, attribute, resource)) {
                // a filter on a sub-attribute other than "value", e.g. Entra ID's emails[type eq "work"].value
                // (RFC 7644 section 3.5.2): select among the entries the resource holds now
                ArrayNode matched = path.select(attribute, currentEntries(resource, model, attribute));

                if (matched.isEmpty()) {
                    // RFC 7644 sections 3.5.2.2, 3.5.2.3 and 3.12: a filter that matches no value is an error. Going on
                    // would let the add half of a replace overwrite a value the filter did not select, such as the
                    // user's only email for emails[type eq "home"].value
                    throw new ScimNoTargetException("No value of " + attribute.getName() + " matches the filter in the path " + rawPath);
                }

                values = matched;
            } else {
                values = path.getValue(attribute);
            }

            if (hasValue && attribute.isMultivalued() && attribute.getComplexType() != null) {
                // the value lists the values to remove, e.g. "members" with [{"value": "<user id>"}], normalized as for add
                values = ofNullable(resolveAttributes(path.getPath(), value).get(attribute)).orElse(NullNode.getInstance());
            }

            setValue(model, attribute, values, REMOVE);
        }
    }

    /**
     * Whether a filtered {@code remove} (or the remove half of a {@code replace}) selects among the entries the
     * resource holds rather than turning the filter into a value. Only for a complex multi-valued attribute of the
     * core schema that the resource carries by default, with a filter naming a sub-attribute other than
     * {@code value}. An attribute returned only on request, such as a group's {@code members}, keeps the
     * {@code value}-only rule, so a filtered remove never reads every member of a large group.
     */
    private boolean selectsByCurrentEntries(Path path, Attribute<M, R> attribute, R resource) {
        return resource != null
                && path.hasFilter()
                && attribute.isMultivalued()
                && attribute.getComplexType() != null
                && !attribute.isExtension()
                && (Attribute.RETURNED_DEFAULT.equals(attribute.getReturned()) || Attribute.RETURNED_ALWAYS.equals(attribute.getReturned()))
                && !path.filtersOnlyOnValue();
    }

    /**
     * The attribute's entries as the model holds them now. They are read from the model rather than from
     * {@code resource}, which is the representation read before the request, so that an earlier operation of the same
     * PATCH request is taken into account (RFC 7644 section 3.5.2 applies the operations in sequence).
     */
    @SuppressWarnings("unchecked")
    private JsonNode currentEntries(R resource, M model, Attribute<M, R> attribute) {
        String modelAttributeName = attribute.getModelAttributeName();

        if (modelAttributeName == null) {
            return null;
        }

        R current;

        try {
            current = (R) resource.getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create instance of resource type " + resource.getClass(), e);
        }

        attribute.set(current, getAttributeValue(model, modelAttributeName));

        JsonNode representation = JsonSerialization.mapper.valueToTree(current);
        return representation == null ? null : representation.get(attribute.getName());
    }

    @Override
    public Attribute<M, R> getAttributeByPath(String path) {
        Map<Attribute<M, R>, JsonNode> attributes = resolveAttributes(path, NullNode.getInstance());

        if (attributes.isEmpty()) {
            return null;
        }

        if (attributes.size() == 1) {
            return attributes.keySet().iterator().next();
        }

        throw new ModelValidationException("Multiple attributes found for path " + path);
    }

    /**
     * Returns the names of the attributes from the given {@code model}, if provided. The {@code model} provides
     * additional context when resolving model attributes, in case attributes depend on the current model being processed.
     *
     * @return the names of the attributes defined in the model
     */
    protected abstract Set<String> getModelAttributeNames();

    /**
     * Returns the value of the attribute with the given {@code name} from the given {@code model}.
     *
     * @param model the model to get the attribute value from
     * @param name the name of the attribute to get the value from
     * @return the value of the attribute with the given name from the model
     */
    protected abstract Object getAttributeValue(M model, String name);

    /**
     * Returns the name of the attribute in the schema for the attribute with the given {@code name} from the given {@code model}.
     *
     * @param name the name of the attribute to get the schema name from
     * @return the name of the attribute in the schema for the attribute with the given name from the model
     */
    protected abstract String getAttributeSchemaName(String name);

    private void populateModel(M model, R resource) {
        ObjectNode resourceNode;

        try {
            resourceNode = JsonSerialization.createObjectNode(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert representation to JSON", e);
        }

        for (Entry<String, Attribute<M, R>> entry : getAttributes().entrySet()) {
            Attribute<M, R> attribute = entry.getValue();
            String scimName = attribute.getName();
            ObjectNode valueNode = resourceNode;

            if (attribute.isExtension()) {
                JsonNode node = ofNullable(resourceNode.get(attribute.getSchema())).orElse(NullNode.getInstance());

                if (!node.isObject()) {
                    continue;
                }

                valueNode = (ObjectNode) node;
                scimName = attribute.getSimpleName();
            }

            Object value = getJsonValue(valueNode, scimName);

            if (value != null) {
                setValue(model, attribute, value);
            }
        }
    }

    private void populateResourceType(R resource, M model, List<String> requestedAttributes, List<String> excludedAttributes) {
        for (Entry<String, Attribute<M, R>> entry : getAttributes().entrySet()) {
            Attribute<M, R> attribute = entry.getValue();

            if (!attribute.isExcluded(this, requestedAttributes, excludedAttributes)) {
                String modelAttributeName = attribute.getModelAttributeName();
                Object value = getAttributeValue(model, modelAttributeName);

                attribute.set(resource, value);

                if (!isInternal()) {
                    resource.addSchema(this.id);
                }
            }
        }
    }

    protected Map<String, Attribute<M,R>> getAttributeMappers() {
        Map<String, Attribute<M,R>> mappers = new HashMap<>();

        for (String name : getModelAttributeNames()) {
            Attribute<M, R> attribute = getAttributeMapperByModelAttribute(name);

            if (attribute != null) {
                mappers.put(attribute.getName(), attribute);
            }
        }

        return mappers;
    }

    protected Attribute<M, R> getAttributeMapperByModelAttribute(String name) {
        String scimName = getAttributeSchemaName(name);

        if (scimName == null) {
            return null;
        }

        Attribute<M, R> attribute = getAttributes().get(scimName);

        if (attribute != null) {
            return attribute;
        }

        if (!isCore() && scimName.startsWith(getId())) {
            scimName = scimName.substring(getId().length() + 1);
        }

        for (Entry<String, Attribute<M, R>> entry : getAttributes().entrySet()) {
            Attribute<M, R> attr = entry.getValue();
            String parent = attr.getParentName();

            if (parent != null && entry.getKey().equals(parent + "." + scimName)) {
                return attr;
            }
        }

        return null;
    }

    private Map<Attribute<M,R>, JsonNode> resolveAttributes(String path, JsonNode valueJson) {
        Objects.requireNonNull(path, "path cannot be null");

        if (valueJson == null) {
            valueJson = NullNode.getInstance();
        }

        Map<Attribute<M, R>, JsonNode> attributes = new HashMap<>();
        // try resolve a direct reference to an attribute first
        Attribute<M, R> attribute = getAttributes().get(path);

        if (attribute == null) {
            for (Entry<String, Attribute<M, R>> entry : getAttributes().entrySet()) {
                Attribute<M, R> attr = entry.getValue();

                if (hasPath(attr, path)) {
                    if (valueJson.isObject() && attr.isMultivalued() && attr.getComplexType() != null && attr.equals(getAttributes().get(attr.getName()))) {
                        // another spelling of the attribute name, e.g. "Members": resolve the object as for "members" instead of unwrapping it
                        return resolveAttributes(attr.getName(), valueJson);
                    }
                    return Map.of(attr, resolveAttributeValue(attr, valueJson));
                }
            }

            if (valueJson.isObject()) {
                for (Entry<String, JsonNode> property : valueJson.properties()) {
                    Attribute<M, R> attr = getAttributes().get(path + "." + property.getKey());

                    if (attr != null) {
                        // found sub-attribute withing the path
                        attributes.put(attr, property.getValue());
                    } else if (isCore() && getId().equals(path)) {
                        // if core schema, resolve all its attributes based on the properties of the value JSON node
                        attributes.putAll(resolveAttributes(property.getKey(), property.getValue()));
                    } else {
                        // fallback to resolve the attribute from an extension schema
                        String name = property.getKey();

                        if (!name.startsWith(getId())) {
                            name = getId() + ":" + name;
                        }

                        attributes.putAll(resolveAttributes(name, property.getValue()));
                    }
                }
            }
        } else {
            if (valueJson.isObject()) {
                if (valueJson.has(path)) {
                    return resolveAttributes(path, valueJson.get(path));
                }

                Class<?> complexType = attribute.getComplexType();

                if (complexType != null && attribute.isMultivalued()) {
                    attributes.put(attribute, valueJson);
                }

                return attributes;
            }

            // path is an attribute, value must be the value of the attribute
            return Map.of(attribute, resolveAttributeValue(attribute, valueJson));
        }

        return attributes;
    }

    protected boolean hasPath(Attribute<M, R> attribute, String path) {
        if (attribute == null || path == null) {
            return false;
        }

        return resolvePaths(attribute).stream().anyMatch(path::equalsIgnoreCase);
    }

    private List<String> resolvePaths(Attribute<M, R> attr) {
        List<String> paths = new ArrayList<>();

        // the name of the attribute itself is always a valid path
        paths.add(attr.getName());

        if (attr.isExtension()) {
            paths.add(attr.getSchema() + ":" + attr.getSimpleName());
        }

        if (attr.getName().endsWith(".value")) {
            paths.add(attr.getSchema() + ":" + attr.getSimpleName().substring(0, attr.getSimpleName().length() - 6));
        }

        if (attr.getAlias() != null) {
            paths.add(getId() + ":" + attr.getAlias());
        }

        Class<?> complexType = attr.getComplexType();

        if (complexType != null) {
            if (MultiValuedAttribute.class.isAssignableFrom(complexType)) {
                paths.add(attr.getName() + ".value");
            }
        }

        return paths;
    }

    private void setValue(M model, Attribute<M, R> attribute, Object value) {
        setValue(model, attribute, value, SET);
    }

    private void setValue(M model, Attribute<M, R> attribute, Object value, Operation operation) {
        Objects.requireNonNull(model, "model cannot be null");
        Objects.requireNonNull(attribute, "attribute cannot be null");
        Objects.requireNonNull(operation, "operation cannot be null");

        if (attribute.isImmutable()) {
            String modelAttrName = attribute.getModelAttributeName();
            Object currentValue = modelAttrName == null ? null : getAttributeValue(model, modelAttrName);

            if (operation == SET) {
                // PUT/create: RFC 7644 §3.5.1 only rejects the request if the attribute already has a value
                // and the incoming value differs from it; re-submitting the same value is allowed.
                if (currentValue != null && !valuesEqual(attribute, currentValue, value)) {
                    throw new ScimMutabilityException(
                            "Attribute '" + attribute.getName() + "' is immutable");
                }
            } else {
                if (modelAttrName == null || currentValue != null) {
                    throw new ScimMutabilityException(
                            "Attribute '" + attribute.getName() + "' is immutable");
                }
                if (operation == REMOVE) {
                    return;
                }
            }
        }

        JsonNode jsonValue = toJsonNode(value);

        if (operation != SET && attribute.isMultivalued() && attribute.getComplexType() != null && jsonValue != null && jsonValue.isArray()) {
            for (JsonNode entry : jsonValue) {
                if (!entry.isObject()) {
                    // e.g. "members" with [null] or ["<user id>"] instead of [{"value": "<user id>"}]
                    throw new ModelValidationException("Each entry of " + attribute.getName() + " must be an object");
                }
            }
        }

        switch (operation) {
            case SET -> attribute.set(model, jsonValue);
            case ADD -> attribute.add(model, jsonValue);
            case REMOVE -> attribute.remove(model, jsonValue);
            default -> throw new ModelException("Invalid operation: " + operation);
        }
    }

    /**
     * Compares the current model value of an immutable attribute with the incoming value, to determine whether a
     * PUT is actually attempting to change it. Timestamp attributes are normalized to epoch milliseconds before
     * comparing, since the model stores them as {@link Long} while the wire representation is an ISO 8601 string.
     */
    private boolean valuesEqual(Attribute<M, R> attribute, Object currentValue, Object incomingValue) {
        if (attribute.isTimestamp()) {
            Long incoming;

            try {
                incoming = incomingValue instanceof Long l ? l : ScimDateTimeUtil.parseDateTime(incomingValue.toString());
            } catch (IllegalArgumentException e) {
                throw new ModelValidationException(e.getMessage());
            }

            Long current = currentValue instanceof Long l ? l : Long.valueOf(currentValue.toString());
            return current.equals(incoming);
        }

        return String.valueOf(currentValue).equals(String.valueOf(incomingValue));
    }

    private JsonNode toJsonNode(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof JsonNode) {
            return (JsonNode) value;
        } else if (value instanceof Collection<?> values) {
            ArrayNode nodes = JsonNodeFactory.instance.arrayNode();

            for (Object v : values) {
                if (v instanceof JsonNode jsonNode) {
                    nodes.add(jsonNode);
                } else {
                    nodes.add(TextNode.valueOf(v.toString()));
                }
            }

            return nodes;
        }

        return TextNode.valueOf(value.toString());
    }

    private JsonNode resolveAttributeValue(Attribute<M, R> attribute, JsonNode jsonNode) {
        if (jsonNode.isValueNode()) {
            Class<?> complexType = attribute.getComplexType();

            if (complexType != null) {
                if (MultiValuedAttribute.class.isAssignableFrom(complexType)) {
                    ObjectNode objectNode = JsonSerialization.createObjectNode();
                    objectNode.set("value", jsonNode);
                    return objectNode;
                }

                throw new ModelValidationException("Unsupported complex type for attribute: " + attribute.getName());
            }

            // return fast if a value node
            return jsonNode;
        }

        String name = attribute.getName();

        if (jsonNode.isObject()) {
            if (jsonNode.has("value")) {
                // if there is a "value" property, we assume it is a multivalued attribute and we take the value of the "value" property as the value of the attribute
                return jsonNode.get("value");
            }
            // iterate of all properties of the object to find the specific value for the property with the given name
            for (Entry<String, JsonNode> property : jsonNode.properties()) {
                if (property.getKey().equals(name)) {
                    return resolveAttributeValue(attribute, property.getValue());
                }
            }
        } else if (jsonNode.isArray() && !jsonNode.isEmpty()) {
            if (attribute.isMultivalued()) {
                return jsonNode;
            }
            // single valued attribute, we take the first value of the array as the value of the attribute
            return resolveAttributeValue(attribute, jsonNode.get(0));
        }

        return NullNode.getInstance();
    }
}
