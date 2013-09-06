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

    // TODO: Thread-safety support (maybe...)
    private Map<ConverterKey, Converter<?, ?>> converterRegistry = new HashMap<ConverterKey, Converter<?, ?>>();

    public <T, S> void addConverter(Converter<T, S> converter) {
        ConverterKey converterKey = new ConverterKey(converter.getApplicationObjectType(), converter.getDBObjectType());
        converterRegistry.put(converterKey, converter);
    }

    public <T, S> T convertDBObjectToApplicationObject(S dbObject, Class<T> expectedApplicationObjectType) {
        // TODO: Not type safe as it expects that S type of converter must exactly match type of dbObject. Converter lookup should be more flexible
        Class<S> expectedDBObjectType = (Class<S>)dbObject.getClass();
        Converter<T, S> converter = getConverter(expectedApplicationObjectType, expectedDBObjectType);
        return converter.convertDBObjectToApplicationObject(dbObject);
    }

    public <T, S> S convertApplicationObjectToDBObject(T applicationObject, Class<S> expectedDBObjectType) {
        // TODO: Not type safe as it expects that T type of converter must exactly match type of applicationObject. Converter lookup should be more flexible
        Class<T> expectedApplicationObjectType = (Class<T>)applicationObject.getClass();
        Converter<T, S> converter = getConverter(expectedApplicationObjectType, expectedDBObjectType);

        return converter.convertApplicationObjectToDBObject(applicationObject);
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
