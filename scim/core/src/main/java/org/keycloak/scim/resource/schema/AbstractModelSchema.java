package org.keycloak.scim.resource.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.keycloak.models.Model;
import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.schema.attribute.AttributeMapper;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.utils.JsonUtils.getJsonValue;
import static org.keycloak.utils.StringUtil.isBlank;

public abstract class AbstractModelSchema<M extends Model, R extends ResourceTypeRepresentation> implements ModelSchema<M, R> {

    private final String name;
    private Map<String, Attribute<M, R>> attributes;

    protected AbstractModelSchema(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Attribute<M, R>> getAttributes() {
        if (attributes == null) {
            attributes = doGetAttributes();
        }
        return attributes;
    }

    protected abstract Map<String, Attribute<M,R>> doGetAttributes();

    @Override
    public void populate(M model, R representation) {
        populateModel(model, representation);
        representation.setId(model.getId());
    }

    @Override
    public void populate(R resource, M model) {
        populateResourceType(resource, model);
        resource.setId(model.getId());
    }

    @Override
    public void validate(R representation) throws SchemaValidationException {
        // validate here the schema
    }

    @Override
    public void add(M model, String rawPath, JsonNode value) {
        Objects.requireNonNull(model, "model cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        String path = Optional.ofNullable(rawPath).map(this::formatPath).orElse(getName());
        Map<Attribute<M, R>, JsonNode> attributes = resolveAttributes(path, value);

        for (Entry<Attribute<M, R>, JsonNode> entry : attributes.entrySet()) {
            setValue(model, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void replace(M model, String path, JsonNode value) {
        add(model, path, value);
    }

    @Override
    public void remove(M model, String rawPath) {
        Objects.requireNonNull(model, "model cannot be null");

        if (isBlank(rawPath)) {
            throw new ModelValidationException("Missing path for patch operation remove");
        }

        String path = formatPath(rawPath);

        for (Attribute<M, R> attribute : resolveAttributes(path, null).keySet()) {
            setValue(model, attribute, NullNode.getInstance());
        }
    }

    /**
     * Returns the names of the attributes from the given {@code model}.
     *
     * @param model the model to get the attribute names from
     * @return the names of the attributes defined in the model
     */
    protected abstract Set<String> getAttributeNames(M model);

    /**
     * Returns the value of the attribute with the given {@code name} from the given {@code model}.
     *
     * @param model the model to get the attribute value from
     * @param name the name of the attribute to get the value from
     * @return the value of the attribute with the given name from the model
     */
    protected abstract String getAttributeValue(M model, String name);

    /**
     * Returns the name of the attribute in the schema for the attribute with the given {@code name} from the given {@code model}.
     *
     * @param model the model to get the attribute schema name from
     * @param name the name of the attribute to get the schema name from
     * @return the name of the attribute in the schema for the attribute with the given name from the model
     */
    protected abstract String getAttributeSchemaName(M model, String name);

    private void populateModel(M model, R resource) {
        ObjectNode objectNode;

        try {
            objectNode = JsonSerialization.createObjectNode(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert representation to JSON", e);
        }

        for (String name : getAttributeNames(model)) {
            Attribute<M, R> attribute = getAttributeMapper(model, name);

            if  (attribute == null) {
                continue;
            }

            AttributeMapper<M, R> mapper = attribute.getMapper();

            if (mapper == null) {
                continue;
            }

            Object value = getJsonValue(objectNode, attribute.getName());

            if (value == null) {
                String attributeName = attribute.getName();

                if (attributeName.indexOf('.') > 0) {
                    attributeName = attributeName.substring(attributeName.indexOf('.') + 1);
                    List<String> paths = new ArrayList<>();
                    paths.add(getName());
                    paths.addAll(List.of(attributeName.split("\\.")));
                    value = getJsonValue(objectNode, paths);
                }
            }

            if (value == null) {
                JsonNode schemaExtension = objectNode.get(getName());
                value = getJsonValue(schemaExtension, attribute.getName());
            }

            if (value != null) {
                if (value instanceof Collection<?> values) {
                    for (Object v : values) {
                        if (v instanceof JsonNode jsonNode) {
                            setValue(model, attribute, resolveAttributeValue(attribute.getName(), jsonNode));
                        }
                        // no support for multivalued attributes for now, so we take the first value as the value of the attribute
                        break;
                    }
                } else {
                    mapper.setValue(model, name, value.toString());
                }
            }
        }
    }

    private void populateResourceType(R resource, M model) {
        for (String name : getAttributeNames(model)) {
            Attribute<M, R> attribute = getAttributeMapper(model, name);

            if  (attribute == null) {
                continue;
            }

            AttributeMapper<M, R> mapper = attribute.getMapper();

            if (mapper == null) {
                continue;
            }

            String value = getAttributeValue(model, name);
            mapper.setValue(resource, value);
            resource.addSchema(this.name);
        }
    }

    private Attribute<M, R> getAttributeMapper(M model, String name) {
        String scimName = getAttributeSchemaName(model, name);

        if (scimName == null) {
            return null;
        }

        Attribute<M, R> attribute = getAttributes().get(scimName);

        if (attribute != null) {
            return attribute;
        }

        if (!isCore() && scimName.startsWith(getName())) {
            scimName = scimName.substring(getName().length() + 1);
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

        if (valueJson.isArray()) {
            if (valueJson.isEmpty()) {
                valueJson = NullNode.getInstance();
            } else {
                // for now, only single-valued attributes are supported, so if the value is an array, we take the first element as the value of the attribute
                valueJson = valueJson.get(0);
            }
        }

        Map<Attribute<M, R>, JsonNode> attributes = new HashMap<>();
        // try resolve a direct reference to an attribute first
        Attribute<M, R> attribute = getAttributes().get(path);

        if (attribute == null) {
            for (Entry<String, Attribute<M, R>> entry : getAttributes().entrySet()) {
                Attribute<M, R> attr = entry.getValue();
                List<String> paths = getPaths(attr);

                if (paths.contains(path)) {
                    return Map.of(attr, resolveAttributeValue(attr.getName(), valueJson));
                }
            }

            if (valueJson.isObject()) {
                for (Entry<String, JsonNode> property : valueJson.properties()) {
                    Attribute<M, R> attr = getAttributes().get(path + "." + property.getKey());

                    if (attr != null) {
                        attributes.put(attr, resolveAttributeValue(attr.getName(), property.getValue()));
                    } else if (isCore()) {
                        attributes.putAll(resolveAttributes(property.getKey(), property.getValue()));
                    } else {
                        String name = property.getKey();

                        if (!name.startsWith(getName())) {
                            name = getName() + ":" + name;
                        }

                        attributes.putAll(resolveAttributes(name, property.getValue()));
                    }
                }
            }
        } else {
            if (valueJson.isObject()) {
                // if the value is an object, we assume it is a complex attribute and we iterate over all properties of
                // the object to find the specific value for each sub-attribute
                for (Entry<String, JsonNode> property : valueJson.properties()) {
                    attributes.putAll(resolveAttributes(attribute.getName() + "." + property.getKey(), property.getValue()));
                }
                return attributes;
            }

            // path is an attribute, value must be the value of the attribute
            return Map.of(attribute, resolveAttributeValue(attribute.getName(), valueJson));
        }

        return attributes;
    }

    private String formatPath(String path) {
        int filterStartIdx = path.indexOf("[");

        if (filterStartIdx > 0) {
            int filterEndIdx = path.lastIndexOf("]");

            if (filterEndIdx == -1) {
                throw new RuntimeException("Invalid path: " + path);
            }

            // for now, we do not support filters in the path
            return new StringBuilder(path).delete(filterStartIdx, filterEndIdx + 1).toString();
        }

        return path;
    }

    public void setValue(M model, Attribute<M, R> resolved, JsonNode value) {
        for (String name : getAttributeNames(model)) {
            Attribute<M, R> attribute = getAttributeMapper(model, name);

            // no mapper found, or not the same as resolved, or is not the parent of the resolved attribute, so we skip
            if (attribute == null || !(attribute.getName().equals(resolved.getName()) || attribute.isParent(resolved))) {
                continue;
            }

            AttributeMapper<M, R> mapper = resolved.getMapper();
            mapper.setValue(model, name, value.isNull() ? null : value.asText());
        }
    }

    private JsonNode resolveAttributeValue(String name, JsonNode jsonNode) {
        if (jsonNode.isValueNode()) {
            // return fast if a value node
            return jsonNode;
        }

        if (jsonNode.isObject()) {
            if (jsonNode.has("value")) {
                // if there is a "value" property, we assume it is a multivalued attribute and we take the value of the "value" property as the value of the attribute
                return jsonNode.get("value");
            }
            // iterate of all properties of the object to find the specific value for the property with the given name
            for (Entry<String, JsonNode> property : jsonNode.properties()) {
                if (property.getKey().equals(name)) {
                    return resolveAttributeValue(name, property.getValue());
                }
            }
        } else if (jsonNode.isArray() && !jsonNode.isEmpty()) {
            // we do not support multivalued attributes for now, so if the value is an array, we take the first element as the value of the attribute
            return resolveAttributeValue(name, jsonNode.get(0));
        }

        return NullNode.getInstance();
    }

    @Override
    public Attribute<M, R> resolveAttribute(String name) {
        Map<Attribute<M, R>, JsonNode> attributes = resolveAttributes(name, NullNode.getInstance());

        if (attributes.isEmpty()) {
            return null;
        }

        return attributes.keySet().iterator().next();
    }

    public List<String> getPaths(Attribute<M, R> attr) {
        List<String> paths = new ArrayList<>();

        String parent = attr.getParentName();

        if (parent != null && !isCore()) {
            paths.add(getName() + attr.getName().replace(parent + ".", ":"));
        }

        if (attr.getAlias() != null) {
            if(!isCore()) {
                paths.add(getName() + ":" + attr.getAlias());
            }
            paths.add(attr.getParentName());
        }

        paths.add(attr.getName());

        return paths;
    }
}
