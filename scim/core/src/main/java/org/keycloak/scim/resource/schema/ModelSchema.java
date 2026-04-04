package org.keycloak.scim.resource.schema;

import java.util.List;
import java.util.Map;

import org.keycloak.models.Model;
import org.keycloak.scim.resource.ResourceTypeRepresentation;
import org.keycloak.scim.resource.schema.attribute.Attribute;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>An interface that represents a schema for a resource type.
 *
 * <p>A schema is a set of metadata that basically describes the attributes of a resource type. It is used to validate
 * the representation of a resource type and to validate and map the attributes from the schema from a {@link ResourceTypeRepresentation}, usually an
 * object from the RESTful layer, to a {@link Model} and vice versa.
 */
public interface ModelSchema<M extends Model, R extends ResourceTypeRepresentation> {

    /**
     * The id of the schema. It is used to identify the schema and to associate it with a resource type.
     *
     * @return the id of the schema
     */
    String getId();

    String getName();

    String getDescription();

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
     * Populates the given {@code representation} with the attributes from the given {@code model},
     * filtering based on the {@code attributes} and {@code excludedAttributes} parameters.
     *
     * @param representation the representation to populate
     * @param model the model to populate from
     * @param attributes the list of attributes to include (may be null for no inclusion filter)
     * @param excludedAttributes the list of attributes to exclude (may be null for no exclusion filter)
     */
    default void populate(R representation, M model, List<String> attributes, List<String> excludedAttributes) {
        populate(representation, model);
    }

    /**
     * Validates the given {@code representation} against the schema. It should throw an exception if the representation is not valid.
     *
     * @param representation the representation to be validated
     * @throws SchemaValidationException if the representation is not valid against the schema
     */
    void validate(R representation) throws SchemaValidationException;

    /**
     * Performs a PATCH {@code add} operation on the given {@code model} for the attribute defined by the given {@code path} and the given {@code value}.
     *
     * @param model the model to perform the operation on
     * @param path the path of the attribute to perform the operation on. It can be null if the operation is performed on the whole resource.
     * @param value the value
     */
    default void add(M model, String path, JsonNode value) {
        throw new UnsupportedOperationException("Add operation is not supported for this schema");
    }

    /**
     * Performs a PATCH {@code remove} operation on the given {@code model} for the attribute defined by the given {@code path}.
     *
     * @param resource the resource to perform the operation on
     * @param model the model to perform the operation on
     * @param path the path of the attribute to perform the operation on
     */
    default void remove(R resource, M model, String path) {
        throw new UnsupportedOperationException("Add operation is not supported for this schema");
    }

    /**
     * Performs a PATCH {@code replace} operation on the given {@code model} for the attribute defined by the given {@code path}.
     *
     * @param resource the resource to perform the operation on
     * @param model the model to perform the operation on
     * @param path the path of the attribute to perform the operation on
     */
    default void replace(R resource, M model, String path, JsonNode value) {
        if (path != null) {
            remove(resource, model, path);
        }
        add(model, path, value);
    }

    /**
     * Returns {@code true} if this schema is a core schema.
     *
     * @return {@code true} if this schema is a core schema, {@code false} otherwise
     */
    default boolean isCore() {
        return true;
    }

    /**
     * Returns an {@link Attribute} defined by this schema for the given {@code path}.
     * The path can be {@code null} if the attribute is the whole resource.
     * It can also be a dot-separated path to a sub-attribute, e.g. "name.familyName".
     *
     * @param path the path
     * @return the attribute for the given path, or {@code null} if no attribute is defined for the given path
     */
    Attribute<M, R> getAttributeByPath(String path);
}
