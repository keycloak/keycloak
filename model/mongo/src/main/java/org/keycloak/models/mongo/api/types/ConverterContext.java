package org.keycloak.models.mongo.api.types;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConverterContext<T> {

    // object to convert
    private final T objectToConvert;

    // expected return type, which could be useful information in some converters, so they are able to dynamically instantiate types
    private final Class<?> expectedReturnType;

    // in case that expected return type is generic type (like "List<String>"), then genericTypes could contain list of expected generic arguments
    private final List<Class<?>> genericTypes;

    public ConverterContext(T objectToConvert, Class<?> expectedReturnType, List<Class<?>> genericTypes) {
        this.objectToConvert = objectToConvert;
        this.expectedReturnType = expectedReturnType;
        this.genericTypes = genericTypes;
    }

    public T getObjectToConvert() {
        return objectToConvert;
    }

    public Class<?> getExpectedReturnType() {
        return expectedReturnType;
    }

    public List<Class<?>> getGenericTypes() {
        return genericTypes;
    }
}
