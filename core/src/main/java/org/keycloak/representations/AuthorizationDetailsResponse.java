/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.representations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic response object for authorization details processing.
 * This class serves as a base for different types of authorization details responses
 * from various RAR (Rich Authorization Requests) implementations.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class AuthorizationDetailsResponse extends AuthorizationDetailsJSONRepresentation {

    // Map of parsers for specific values of "type" claim of authorizationDetails
    private static final Map<String, AuthorizationDetailsResponseParser<?>> PARSERS = new ConcurrentHashMap<>();

    /**
     * Register new parser for specific type. This can be later used by {@link #asSubtype} method. Parsers are supposed to be
     * registered before authorizationDetails are being used by the application and before method {@link #asSubtype} is called for the first time.
     * Usually it is supposed to be registered at the startup of the application. If implementing Keycloak provider <em>AuthorizationDetailsProcessor</em>, it might
     * be good to register corresponding parser in the <em>AuthorizationDetailsProcessorFactory.init</em> method of your provider
     *
     * @param type
     * @param parser
     */
    public static void registerParser(String type, AuthorizationDetailsResponseParser<?> parser) {
        PARSERS.put(type, parser);
    }

    /**
     * @param clazz Subtype of {@link AuthorizationDetailsResponse}, which will be returned by calling this method
     * @return this authorizationDetails content cast to the class specified by clazz parameter as long as parser corresponding to the type returned by {@link #getType}
     * is able to parse this authorizationDetails and convert it to that subtype
     */
    public <T extends AuthorizationDetailsResponse> T asSubtype(Class<T> clazz) {
        AuthorizationDetailsResponseParser<T> parser = (AuthorizationDetailsResponseParser<T>) PARSERS.get(getType());
        if (parser == null) {
            throw new IllegalArgumentException("Unsupported to parse response of type '" + getType() + "' to the type '" + clazz +
                    "'. Please make sure that corresponding parser is registered.");
        }
        return parser.asSubtype(this);
    }

    /**
     * Parser, which is able to create specific subtype of {@link AuthorizationDetailsResponse} in performant way
     */
    public interface AuthorizationDetailsResponseParser<T extends AuthorizationDetailsResponse> {

        T asSubtype(AuthorizationDetailsResponse response);

    }
}
