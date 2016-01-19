package org.keycloak.connections.mongo.impl.types;

import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class StringToEnumMapper implements Mapper<String, Enum> {

    @Override
    public Enum convertObject(MapperContext<String, Enum> context) {
        String enumValue = context.getObjectToConvert();

        Class<? extends Enum> clazz = context.getExpectedReturnType();
        return Enum.valueOf(clazz, enumValue);
    }

    @Override
    public Class<? extends String> getTypeOfObjectToConvert() {
        return String.class;
    }

    @Override
    public Class<Enum> getExpectedReturnType() {
        return Enum.class;
    }
}
