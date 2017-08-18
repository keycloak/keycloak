package org.keycloak.adapters.springboot.client;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.web.client.RestTemplate;

public class KeycloakRestTemplateCustomizer implements RestTemplateCustomizer {

    private final KeycloakSecurityContextClientRequestInterceptor keycloakInterceptor;

    public KeycloakRestTemplateCustomizer() {
        this(new KeycloakSecurityContextClientRequestInterceptor());
    }

    protected KeycloakRestTemplateCustomizer(
            KeycloakSecurityContextClientRequestInterceptor keycloakInterceptor
    ) {
        this.keycloakInterceptor = keycloakInterceptor;
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add(keycloakInterceptor);
    }
}
