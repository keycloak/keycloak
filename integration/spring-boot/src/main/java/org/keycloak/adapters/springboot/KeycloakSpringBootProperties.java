package org.keycloak.adapters.springboot;

import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak", ignoreUnknownFields = false)
public class KeycloakSpringBootProperties extends AdapterConfig {

}
