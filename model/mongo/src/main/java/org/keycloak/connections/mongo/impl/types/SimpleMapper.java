package org.keycloak.connections.mongo.impl.types;

import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;

/**
 * Just returns input
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleMapper<T> implements Mapper<T, T> {

    private final Class<T> expectedType;

    public SimpleMapper(Class<T> expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public T convertObject(MapperContext<T, T> context) {
        T objectToConvert = context.getObjectToConvert();
        return objectToConvert;
    }

    @Override
    public Class<? extends T> getTypeOfObjectToConvert() {
        return expectedType;
    }

    @Override
    public Class<T> getExpectedReturnType() {
        return expectedType;
    }
}
