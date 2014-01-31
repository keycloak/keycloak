package org.keycloak.models.mongo.impl.types;

import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;

/**
 * Just returns input
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleConverter<T> implements Converter<T, T> {

    private final Class<T> expectedType;

    public SimpleConverter(Class<T> expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public T convertObject(ConverterContext<T> context) {
        T objectToConvert = context.getObjectToConvert();
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
