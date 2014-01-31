package org.keycloak.models.mongo.api.types;

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
    // Converters of Application objects to DB objects
    private Map<Class<?>, Converter<?, ?>> appObjectConverters = new HashMap<Class<?>, Converter<?, ?>>();

    // Converters of DB objects to Application objects
    private Map<Class<?>, Map<Class<?>, Converter<?, ?>>> dbObjectConverters = new HashMap<Class<?>, Map<Class<?>, Converter<?,?>>>();


    /**
     * Add converter for converting application objects to DB objects
     *
     * @param converter
     */
    public void addAppObjectConverter(Converter<?, ?> converter) {
        appObjectConverters.put(converter.getConverterObjectType(), converter);
    }


    /**
     * Add converter for converting DB objects to application objects
     *
     * @param converter
     */
    public void addDBObjectConverter(Converter<?, ?> converter) {
        Class<?> dbObjectType = converter.getConverterObjectType();
        Class<?> appObjectType = converter.getExpectedReturnType();
        Map<Class<?>, Converter<?, ?>> appObjects = dbObjectConverters.get(dbObjectType);
        if (appObjects == null) {
            appObjects = new HashMap<Class<?>, Converter<?, ?>>();
            dbObjectConverters.put(dbObjectType, appObjects);
        }
        appObjects.put(appObjectType, converter);
    }


    public Object convertDBObjectToApplicationObject(ConverterContext<Object> context) {
        Object dbObject = context.getObjectToConvert();
        Class<?> expectedApplicationObjectType = context.getExpectedReturnType();

        Class<?> dbObjectType = dbObject.getClass();
        Converter<Object, Object> converter;

        Map<Class<?>, Converter<?, ?>> appObjects = dbObjectConverters.get(dbObjectType);
        if (appObjects == null) {
            throw new IllegalArgumentException("Not found any converters for type " + dbObjectType);
        } else {
            if (appObjects.size() == 1) {
                converter = (Converter<Object, Object>)appObjects.values().iterator().next();
            } else {
                // Try to find converter for requested application type
                converter = (Converter<Object, Object>)getAppConverterForType(context.getExpectedReturnType(), appObjects);
            }
        }

        if (converter == null) {
            throw new IllegalArgumentException("Can't found converter for type " + dbObjectType + " and expectedApplicationType " + expectedApplicationObjectType);
        }
        /*if (!expectedApplicationObjectType.isAssignableFrom(converter.getExpectedReturnType())) {
            throw new IllegalArgumentException("Converter " + converter + " has return type " + converter.getExpectedReturnType() +
                    " but we need type " + expectedApplicationObjectType);
        } */

        return converter.convertObject(context);
    }


    public <S> S convertApplicationObjectToDBObject(Object applicationObject, Class<S> expectedDBObjectType) {
        Class<?> appObjectType = applicationObject.getClass();
        Converter<Object, S> converter = (Converter<Object, S>)getAppConverterForType(appObjectType, appObjectConverters);
        if (converter == null) {
            throw new IllegalArgumentException("Can't found converter for type " + appObjectType + " in registered appObjectConverters");
        }
        if (!expectedDBObjectType.isAssignableFrom(converter.getExpectedReturnType())) {
            throw new IllegalArgumentException("Converter " + converter + " has return type " + converter.getExpectedReturnType() +
                    " but we need type " + expectedDBObjectType);
        }
        return converter.convertObject(new ConverterContext(applicationObject, expectedDBObjectType, null));
    }

    // Try to find converter for given type or all it's supertypes
    private static Converter<Object, ?> getAppConverterForType(Class<?> appObjectType, Map<Class<?>, Converter<?, ?>> appObjectConverters) {
        Converter<Object, ?> converter = (Converter<Object, ?>)appObjectConverters.get(appObjectType);
        if (converter != null) {
            return converter;
        } else {
            Class<?>[] interfaces = appObjectType.getInterfaces();
            for (Class<?> interface1 : interfaces) {
                converter = getAppConverterForType(interface1, appObjectConverters);
                if (converter != null) {
                    return converter;
                }
            }

            Class<?> superType = appObjectType.getSuperclass();
            if (superType != null) {
                return getAppConverterForType(superType, appObjectConverters);
            } else {
                return null;
            }
        }
    }
}
