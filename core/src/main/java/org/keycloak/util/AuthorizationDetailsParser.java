package org.keycloak.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;

/**
 * Parser, which is able to create specific subtype of {@link AuthorizationDetailsJSONRepresentation} in performant way
 */
public interface AuthorizationDetailsParser {

    <T extends AuthorizationDetailsJSONRepresentation> T asSubtype(AuthorizationDetailsJSONRepresentation authzDetail, Class<T> clazz);


    Map<String, AuthorizationDetailsParser> PARSERS = new ConcurrentHashMap<>();

    /**
     * Register new parser for specific type. This can be later used by {@link #asSubtype} method. Parsers are supposed to be
     * registered before authorizationDetails are being used by the application and before method {@link #asSubtype} is called for the first time.
     * Usually it is supposed to be registered at the startup of the application. If implementing Keycloak provider <em>AuthorizationDetailsProcessor</em>, it might
     * be good to register corresponding parser in the <em>AuthorizationDetailsProcessorFactory.init</em> method of your provider
     *
     * @param type Type as used in the "type" claim of "authorization_details" object entry
     * @param parser Parser for this type
     */
    static void registerParser(String type, AuthorizationDetailsParser parser) {
        PARSERS.put(type, parser);
    }

    /**
     * Method is not supposed to be called directly. Rather please make sure to use {@link #registerParser(String, AuthorizationDetailsParser)} and
     * then use {@link #asSubtype(AuthorizationDetailsJSONRepresentation, Class)} to call directly from the application
     *
     * @param authzDetail Authorization detail object to cast
     * @param clazz Subtype of {@link AuthorizationDetailsJSONRepresentation}, which will be returned by calling this method
     * @return given authzDetail passed in <em>authzDetail</em> parameter cast to the class specified by clazz parameter as long as parser corresponding to the type
     * returned by {@link AuthorizationDetailsJSONRepresentation#getType} is able to parse this authorizationDetails and convert it to that subtype
     */
    static <T extends AuthorizationDetailsJSONRepresentation> T parseToSubtype(AuthorizationDetailsJSONRepresentation authzDetail, Class<T> clazz) {
        if (authzDetail.getType() == null) {
            throw new IllegalArgumentException("Used authzDetail entry does not have 'type' set. The used authzDetail entry was: " + authzDetail);
        }
        AuthorizationDetailsParser parser = PARSERS.get(authzDetail.getType());
        if (parser == null) {
            throw new IllegalArgumentException("Unsupported to parse response of type '" + authzDetail.getType() + "' to the class '" + clazz +
                    "'. Please make sure that corresponding parser is registered.");
        }
        return parser.asSubtype(authzDetail, clazz);
    }
}
