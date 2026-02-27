package org.keycloak.scim.resource.spi;

import java.util.stream.Stream;

import org.keycloak.models.ModelValidationException;
import org.keycloak.scim.resource.ResourceTypeRepresentation;

/**
 * A specialization of {@link ScimResourceTypeProvider} for singleton SCIM resource types.
 *
 * <p>Singleton resource types represent resources of which only a single instance exists within the SCIM service provider.
 * Examples of singleton resource types include the {@link org.keycloak.scim.resource.config.ServiceProviderConfig}.
 *
 * <p>This interface does not add any new methods to {@link ScimResourceTypeProvider}, but serves as a marker interface
 * to indicate that the implementing provider manages a singleton resource type. Implementations of this interface should
 * ensure that their behavior aligns with the semantics of singleton resources, such as returning a single instance
 * in retrieval operations and handling creation and deletion appropriately.
 */
public interface SingletonResourceTypeProvider<R extends ResourceTypeRepresentation> extends ScimResourceTypeProvider<R> {

    R getSingleton();

    @Override
    default R create(R resource) {
        throw unsupportedOperation();
    }

    @Override
    default R update(R user) {
        throw unsupportedOperation();
    }

    @Override
    default R get(String id) {
        throw unsupportedOperation();
    }

    @Override
    default Stream<R> getAll() {
        return Stream.of(getSingleton());
    }

    @Override
    default boolean delete(String id) {
        throw unsupportedOperation();
    }

    private ModelValidationException unsupportedOperation() {
        return new ModelValidationException("Unsupported operation for resource type " + getResourceType());
    }

    @Override
    default void close() {
    }
}
