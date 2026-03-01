package org.keycloak.scim.resource.schema;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.models.Model;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.schema.attribute.AttributeMapper;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.scim.resource.Scim.getCoreSchema;
import static org.keycloak.utils.JsonUtils.getJsonValue;

public abstract class AbstractModelSchema<M extends Model, R extends ResourceTypeRepresentation> implements ModelSchema<M, R> {

    private final String name;
    private final Map<String, Attribute<M, R>> attributes;

    protected AbstractModelSchema(String name, List<Attribute<M, R>> attributes) {
        this.name = name;
        this.attributes = attributes.stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Attribute<M, R>> getAttributes() {
        return attributes;
    }

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

    /**
     * Returns the name of the schema for the attribute with the given {@code name} from the given {@code model}.
     * It is used to determine which schema the attribute belongs to.
     *
     * @param model the model to get the attribute schema from
     * @param name the name of the attribute to get the schema from
     * @return the name of the schema for the attribute with the given name from the model
     */
    protected abstract String getAttributeSchema(M model, String name);

    private void populateModel(M model, R resource) {
        ObjectNode objectNode;

        try {
            objectNode = JsonSerialization.createObjectNode(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert representation to JSON", e);
        }

        for (String name : getAttributeNames(model)) {
            Attribute<M, R> attribute = getAttributeMapper(model, resource, name);

            if  (attribute == null) {
                continue;
            }

            AttributeMapper<M, R> mapper = attribute.getMapper();

            if (mapper == null) {
                continue;
            }

            Object value = getJsonValue(objectNode, attribute.getName());

            if (value == null) {
                JsonNode schemaExtension = objectNode.get(getName());
                value = getJsonValue(schemaExtension, attribute.getName());
            }

            if (value != null) {
                mapper.setValue(model, name, value.toString());
            }
        }
    }

    private void populateResourceType(R resource, M model) {
        for (String name : getAttributeNames(model)) {
            Attribute<M, R> attribute = getAttributeMapper(model, resource, name);

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

    private Attribute<M, R> getAttributeMapper(M model, R resource, String name) {
        Object schema = getAttributeSchema(model, name);

        if (schema == null) {
            schema = getCoreSchema(resource.getClass());
        }

        if (!this.name.equals(schema)) {
            return null;
        }

        Object scimName = getAttributeSchemaName(model, name);

        if (scimName == null) {
            return null;
        }

        return attributes.get(scimName.toString());
    }
}
