package org.keycloak.adapters.springsecurity.client;

import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Extends Spring's central class for client-side HTTP access, {@link RestTemplate}, adding
 * automatic authentication for service to service calls using the currently authenticated Keycloak principal.
 * This class is designed to work with other services secured by Keycloak.
 *
 * <p>
 *     The main advantage to using this class over Spring's <code>RestTemplate</code> is that authentication
 *     is handled automatically when both the service making the API call and the service being called are
 *     protected by Keycloak authentication.
 * </p>
 *
 * @see RestOperations
 * @see RestTemplate
 *
 * @author Scott Rossillo
 * @version $Revision: 1 $
 */
public class KeycloakRestTemplate extends RestTemplate implements RestOperations {

    /**
     * Create a new instance based on the given {@link KeycloakClientRequestFactory}.
     *
     * @param factory the <code>KeycloakClientRequestFactory</code> to use when creating new requests
     */
    public KeycloakRestTemplate(KeycloakClientRequestFactory factory) {
        super(factory);
    }

}
