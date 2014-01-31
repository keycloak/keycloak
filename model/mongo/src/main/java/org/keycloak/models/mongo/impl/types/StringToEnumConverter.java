package org.keycloak.models.mongo.impl.types;

import org.keycloak.models.mongo.api.types.Converter;
import org.keycloak.models.mongo.api.types.ConverterContext;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class StringToEnumConverter implements Converter<String, Enum> {

    @Override
    public Enum convertObject(ConverterContext<String> context) {
        String enumValue = context.getObjectToConvert();

        Class<? extends Enum> clazz = (Class<? extends Enum>)context.getExpectedReturnType();
        return Enum.valueOf(clazz, enumValue);
    }

    @Override
    public Class<? extends String> getConverterObjectType() {
        return String.class;
    }

    @Override
    public Class<Enum> getExpectedReturnType() {
        return Enum.class;
    }
}
