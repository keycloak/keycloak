package org.keycloak.connections.mongo.api.types;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of mappers, which allow to convert application object to database objects. MapperRegistry is main entry point to be used by application.
 * Application can create instance of MapperRegistry and then register required Mapper objects.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapperRegistry {

    // TODO: Thread-safety support (maybe...)
    // Mappers of Application objects to DB objects
    private Map<Class<?>, Mapper<?, ?>> appObjectMappers = new HashMap<Class<?>, Mapper<?, ?>>();

    // Mappers of DB objects to Application objects
    private Map<Class<?>, Map<Class<?>, Mapper<?, ?>>> dbObjectMappers = new HashMap<Class<?>, Map<Class<?>, Mapper<?,?>>>();


    /**
     * Add mapper for converting application objects to DB objects
     *
     * @param mapper
     */
    public void addAppObjectMapper(Mapper<?, ?> mapper) {
        appObjectMappers.put(mapper.getTypeOfObjectToConvert(), mapper);
    }


    /**
     * Add mapper for converting DB objects to application objects
     *
     * @param mapper
     */
    public void addDBObjectMapper(Mapper<?, ?> mapper) {
        Class<?> dbObjectType = mapper.getTypeOfObjectToConvert();
        Class<?> appObjectType = mapper.getExpectedReturnType();
        Map<Class<?>, Mapper<?, ?>> appObjects = dbObjectMappers.get(dbObjectType);
        if (appObjects == null) {
            appObjects = new HashMap<Class<?>, Mapper<?, ?>>();
            dbObjectMappers.put(dbObjectType, appObjects);
        }
        appObjects.put(appObjectType, mapper);
    }


    public <S> S convertDBObjectToApplicationObject(MapperContext<Object, S> context) {
        Object dbObject = context.getObjectToConvert();
        Class<?> expectedApplicationObjectType = context.getExpectedReturnType();

        Class<?> dbObjectType = dbObject.getClass();
        Mapper<Object, S> mapper;

        Map<Class<?>, Mapper<?, ?>> appObjects = dbObjectMappers.get(dbObjectType);
        if (appObjects == null) {
            throw new IllegalArgumentException("Not found any mappers for type " + dbObjectType);
        } else {
            if (appObjects.size() == 1) {
                mapper = (Mapper<Object, S>)appObjects.values().iterator().next();
            } else {
                // Try to find converter for requested application type
                mapper = (Mapper<Object, S>)getAppConverterForType(context.getExpectedReturnType(), appObjects);
            }
        }

        if (mapper == null) {
            throw new IllegalArgumentException("Can't found mapper for type " + dbObjectType + " and expectedApplicationType " + expectedApplicationObjectType);
        }

        return mapper.convertObject(context);
    }


    public <S> S convertApplicationObjectToDBObject(Object applicationObject, Class<S> expectedDBObjectType) {
        Class<?> appObjectType = applicationObject.getClass();
        Mapper<Object, S> mapper = (Mapper<Object, S>)getAppConverterForType(appObjectType, appObjectMappers);
        if (mapper == null) {
            throw new IllegalArgumentException("Can't found converter for type " + appObjectType + " in registered appObjectMappers");
        }
        if (!expectedDBObjectType.isAssignableFrom(mapper.getExpectedReturnType())) {
            throw new IllegalArgumentException("Converter " + mapper + " has return type " + mapper.getExpectedReturnType() +
                    " but we need type " + expectedDBObjectType);
        }
        return mapper.convertObject(new MapperContext<Object, S>(applicationObject, expectedDBObjectType, null));
    }

    // Try to find converter for given type or all it's supertypes
    private static Mapper<Object, ?> getAppConverterForType(Class<?> appObjectType, Map<Class<?>, Mapper<?, ?>> appObjectConverters) {
        Mapper<Object, ?> mapper = (Mapper<Object, ?>)appObjectConverters.get(appObjectType);
        if (mapper != null) {
            return mapper;
        } else {
            Class<?>[] interfaces = appObjectType.getInterfaces();
            for (Class<?> interface1 : interfaces) {
                mapper = getAppConverterForType(interface1, appObjectConverters);
                if (mapper != null) {
                    return mapper;
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
