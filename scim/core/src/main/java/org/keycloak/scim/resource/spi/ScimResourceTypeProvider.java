package org.keycloak.scim.resource.spi;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.provider.Provider;
import org.keycloak.scim.resource.ResourceTypeRepresentation;

/**
 * <p>A provider of a SCIM resource type.
 *
 * <p>This provider is responsible for the lifecycle of the resource type, including validation, creation, update,
 * retrieval, and deletion of resources of that type. Once registered, the provider will be automatically available from
 * the SCIM API.
 *
 * <p>A {@link ScimResourceTypeProvider}</p> is mainly responsible for mapping values from an SCIM resource representation
 * to the underlying model and vice versa, and for enforcing the rules of the resource type and its corresponding model
 * when managing resource type instances.
 */
public interface ScimResourceTypeProvider<R extends ResourceTypeRepresentation> extends Provider {

    /**
     * Returns the name of the resource type managed by this provider.
     *
     * @return the name of the resource type
     */
    default String getName() {
        return getResourceType().getSimpleName();
    }

    /**
     * Returns the schema name of the resource type managed by this provider.
     *
     * @return the schema URI of the resource type
     */
    String getSchema();

    /**
     * Returns the schema extensions names of the resource type managed by this provider.
     *
     * @return a list of schema extension URIs
     */
    default List<String> getSchemaExtensions() {
        return List.of();
    }

    /**
     * Returns the {@link ResourceTypeRepresentation} type managed by this provider.
     *
     * @return the class of the resource type managed by this provider
     */
    Class<R> getResourceType();

    /**
     * Creates a new resource of this type. This method is invoked after successful validation of the resource,
     * and should persist the resource and return the persisted instance, including any generated identifier or metadata.
     * The returned resource will be used in the response to the client.
     *
     * @param resource the resource to create
     * @return the created resource
     */
    R create(R resource);

    /**
     * Updates an existing resource of this type. This method is invoked after successful validation of the resource,
     * and should persist the updated resource and return the persisted instance.
     * The returned resource will be used in the response to the client.
     *
     * @param user the resource to update
     * @return the updated resource
     */
    R update(R user);

    /**
     * Retrieves a resource of this type by its identifier. This method is invoked when a client requests a specific resource,
     * and should return the resource if it exists, or null if it does not exist.
     * The returned resource will be used in the response to the client.
     *
     * @param id the identifier of the resource to retrieve
     * @return the resource with the given identifier, or null if it does not exist
     */
    R get(String id);

    /**
     * Retrieves all resources of this type. This method is invoked when a client requests a list of resources,
     * and should return a stream of all resources of this type.
     *
     * TODO: this method should support pagination, filtering, and sorting in the future, but for now it returns all resources.
     *
     * @return a stream of all resources of this type
     */
    Stream<R> getAll();

    /**
     * Deletes a resource of this type by its identifier. This method is invoked when a client requests the deletion of a specific resource,
     *
     * @param id the identifier of the resource to delete
     * @return true if the resource was successfully deleted, false if the resource was not found or could not be deleted
     */
    boolean delete(String id);
}
