package org.keycloak.models.mongo.impl.types;

import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EnumToStringConverter implements Converter<Enum, String> {

    // It will be saved in form of "org.keycloak.Gender#MALE" so it's possible to parse enumType out of it
    @Override
    public String convertObject(ConverterContext<Enum> context) {
        Enum objectToConvert = context.getObjectToConvert();

        return objectToConvert.toString();
    }

    @Override
    public Class<? extends Enum> getConverterObjectType() {
        return Enum.class;
    }

    @Override
    public Class<String> getExpectedReturnType() {
        return String.class;
    }
}
