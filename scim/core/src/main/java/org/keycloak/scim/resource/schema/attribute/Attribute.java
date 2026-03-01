package org.keycloak.scim.resource.schema.attribute;

import org.keycloak.models.Model;
import org.keycloak.scim.resource.ResourceTypeRepresentation;

/**
 * Represents an attribute from a {@link org.keycloak.scim.resource.schema.ModelSchema}, its metadata and the mapper
 * that is used to map the attribute from a {@link ResourceTypeRepresentation} to a {@link Model} and vice versa.
 *
 * @see org.keycloak.scim.resource.schema.ModelSchema
 */
public class Attribute<M extends Model, R extends ResourceTypeRepresentation> {

    private final String name;
    private final AttributeMapper<M, R> mapper;

    public Attribute(String name, AttributeMapper<M, R> mapper) {
        this.name = name;
        this.mapper = mapper;
    }

    /**
     * The name of the attribute from the {@link R} representation.
     *
     * @return the name of the attribute
     */
    public String getName() {
        return name;
    }

    /**
     * The mapper that is used to map the attribute from a {@link ResourceTypeRepresentation} to a {@link Model} and vice versa.
     *
     * @return the mapper
     */
    public AttributeMapper<M, R> getMapper() {
        return mapper;
    }
}
