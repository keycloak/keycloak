package org.keycloak.models.mongo.impl.types;

import org.keycloak.models.mongo.api.types.Converter;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleConverter<T> implements Converter<T, T> {

    private final Class<T> expectedType;

    public SimpleConverter(Class<T> expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public T convertObject(T objectToConvert) {
        return objectToConvert;
    }

    @Override
    public Class<? extends T> getConverterObjectType() {
        return expectedType;
    }

    @Override
    public Class<T> getExpectedReturnType() {
        return expectedType;
    }
}
