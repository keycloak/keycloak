package org.keycloak.connections.mongo.api.types;

/**
 * SPI object to convert object from application type to database type and vice versa. Shouldn't be directly used by application.
 * Various mappers should be registered in MapperRegistry, which is main entry point to be used by application
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface Mapper<T, S> {

    /**
     * Convert object from one type to expected type
     *
     * @param mapperContext Encapsulates reference to converted object and other things, which might be helpful in conversion
     * @return converted object
     */
    S convertObject(MapperContext<T, S> mapperContext);

    Class<? extends T> getTypeOfObjectToConvert();

    Class<S> getExpectedReturnType();
}
