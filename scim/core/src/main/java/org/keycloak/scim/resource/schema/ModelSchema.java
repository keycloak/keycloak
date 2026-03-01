package org.keycloak.scim.resource.schema;

import java.util.Map;

import org.keycloak.models.Model;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.attribute.Attribute;

/**
 * <p>An interface that represents a schema for a resource type.
 *
 * <p>A schema is a set of metadata that basically describes the attributes of a resource type. It is used to validate
 * the representation of a resource type and to validate and map the attributes from the schema from a {@link ResourceTypeRepresentation}, usually an
 * object from the RESTful layer, to a {@link Model} and vice versa.
 */
public interface ModelSchema<M extends Model, R extends ResourceTypeRepresentation> {

    /**
     * The name of the schema. It is used to identify the schema and to associate it with a resource type.
     *
     * @return the name of the schema
     */
    String getName();

    /**
     * Returns the attributes defined by this schema. The key of the map is the name of the attribute and the value is
     * the {@link Attribute} that describes the attribute.
     *
     * @return the attributes of the schema
     */
    Map<String, Attribute<M, R>> getAttributes();

    /**
     * Populates the given {@code model} with the attributes from the given {@code representation}.
     *
     * @param model the model to be populated
     * @param representation the representation to populate from
     */
    void populate(M model, R representation);

    /**
     * Populates the given {@code representation} with the attributes from the given {@code model}.
     *
     * @param model the model to be populated
     * @param representation the representation to populate from
     */
    void populate(R representation, M model);

    /**
     * Validates the given {@code representation} against the schema. It should throw an exception if the representation is not valid.
     *
     * @param representation the representation to be validated
     * @throws SchemaValidationException if the representation is not valid against the schema
     */
    void validate(R representation) throws SchemaValidationException;
}
