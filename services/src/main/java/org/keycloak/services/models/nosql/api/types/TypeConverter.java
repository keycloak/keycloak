package org.keycloak.services.models.nosql.api.types;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of converters, which allow to convert application object to database objects. TypeConverter is main entry point to be used by application.
 * Application can create instance of TypeConverter and then register required Converter objects.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TypeConverter {

    private Map<ConverterKey, Converter<?, ?>> converterRegistry = new HashMap<ConverterKey, Converter<?, ?>>();

    public <T, S> void addConverter(Converter<T, S> converter) {
        ConverterKey converterKey = new ConverterKey(converter.getApplicationObjectType(), converter.getDBObjectType());
        converterRegistry.put(converterKey, converter);
    }

    public <T, S> T convertDBObjectToApplicationObject(S dbObject, Class<T> expectedApplicationObjectType, Class<S> expectedDBObjectType) {
        Converter<T, S> converter = getConverter(expectedApplicationObjectType, expectedDBObjectType);
        return converter.convertDBObjectToApplicationObject(dbObject);
    }

    public <T, S> S convertApplicationObjectToDBObject(T applicationobject, Class<T> expectedApplicationObjectType, Class<S> expectedDBObjectType) {
        Converter<T, S> converter = getConverter(expectedApplicationObjectType, expectedDBObjectType);
        return converter.convertApplicationObjectToDBObject(applicationobject);
    }

    private <T, S> Converter<T, S> getConverter( Class<T> expectedApplicationObjectType, Class<S> expectedDBObjectType) {
        ConverterKey key = new ConverterKey(expectedApplicationObjectType, expectedDBObjectType);
        Converter<T, S> converter = (Converter<T, S>)converterRegistry.get(key);

        if (converter == null) {
            throw new IllegalStateException("Can't found converter for expectedApplicationObject=" + expectedApplicationObjectType + ", expectedDBObjectType=" + expectedDBObjectType);
        }

        return converter;
    }


}
