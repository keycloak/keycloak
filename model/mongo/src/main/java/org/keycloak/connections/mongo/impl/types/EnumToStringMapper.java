package org.keycloak.connections.mongo.impl.types;

import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EnumToStringMapper implements Mapper<Enum, String> {

    @Override
    public String convertObject(MapperContext<Enum, String> context) {
        Enum objectToConvert = context.getObjectToConvert();

        return objectToConvert.toString();
    }

    @Override
    public Class<? extends Enum> getTypeOfObjectToConvert() {
        return Enum.class;
    }

    @Override
    public Class<String> getExpectedReturnType() {
        return String.class;
    }
}
