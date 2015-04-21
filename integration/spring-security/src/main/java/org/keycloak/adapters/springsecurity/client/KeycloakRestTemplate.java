package org.keycloak.adapters.springsecurity.client;

import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Created by scott on 4/22/15.
 */
//@Service
//@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class KeycloakRestTemplate extends RestTemplate implements RestOperations {
    public KeycloakRestTemplate(KeycloakClientRequestFactory factory) {
        super(factory);
    }

}
