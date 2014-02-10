package org.keycloak.models.mongo.impl.types;

import org.keycloak.models.mongo.api.types.Mapper;
import org.keycloak.models.mongo.api.types.MapperContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EnumToStringMapper implements Mapper<Enum, String> {

    // It will be saved in form of "org.keycloak.Gender#MALE" so it's possible to parse enumType out of it
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
