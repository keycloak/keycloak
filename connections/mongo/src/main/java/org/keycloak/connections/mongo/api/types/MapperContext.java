package org.keycloak.connections.mongo.api.types;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapperContext<T, S> {

    // object to convert
    private final T objectToConvert;

    // expected return type, which could be useful information in some mappers, so they are able to dynamically instantiate types
    private final Class<? extends S> expectedReturnType;

    // in case that expected return type is generic type (like "List<String>"), then genericTypes could contain list of expected generic arguments
    private final List<Class<?>> genericTypes;

    public MapperContext(T objectToConvert, Class<? extends S> expectedReturnType, List<Class<?>> genericTypes) {
        this.objectToConvert = objectToConvert;
        this.expectedReturnType = expectedReturnType;
        this.genericTypes = genericTypes;
    }

    public T getObjectToConvert() {
        return objectToConvert;
    }

    public Class<? extends S> getExpectedReturnType() {
        return expectedReturnType;
    }

    public List<Class<?>> getGenericTypes() {
        return genericTypes;
    }
}
