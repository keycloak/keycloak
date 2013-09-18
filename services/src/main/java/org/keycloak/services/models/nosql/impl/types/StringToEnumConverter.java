package org.keycloak.services.models.nosql.impl.types;

import org.keycloak.services.models.nosql.api.types.Converter;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class StringToEnumConverter implements Converter<String, Enum> {

    @Override
    public Enum convertObject(String objectToConvert) {
        int index = objectToConvert.indexOf(ClassCache.SPLIT);
        if (index == -1) {
            throw new IllegalStateException("Can't convert enum type with value " + objectToConvert);
        }

        String className = objectToConvert.substring(0, index);
        String enumValue = objectToConvert.substring(index + 3);
        Class<? extends Enum> clazz = (Class<? extends Enum>)ClassCache.getInstance().getOrLoadClass(className);
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
