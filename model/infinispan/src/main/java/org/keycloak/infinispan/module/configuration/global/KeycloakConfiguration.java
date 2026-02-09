package org.keycloak.infinispan.module.configuration.global;

import org.keycloak.models.KeycloakSessionFactory;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;

@BuiltBy(KeycloakConfigurationBuilder.class)
public class KeycloakConfiguration {

    static final AttributeDefinition<KeycloakSessionFactory> KEYCLOAK_SESSION_FACTORY = AttributeDefinition.builder("keycloak-session-factory", null, KeycloakSessionFactory.class)
            .global(true)
            .autoPersist(false)
            .immutable()
            .build();

    private final AttributeSet attributes;

    static AttributeSet attributeSet() {
        return new AttributeSet(KeycloakConfiguration.class, KEYCLOAK_SESSION_FACTORY);
    }

    KeycloakConfiguration(AttributeSet attributes) {
        this.attributes = attributes;
    }

    AttributeSet attributes() {
        return attributes;
    }

    public KeycloakSessionFactory keycloakSessionFactory() {
        return attributes.attribute(KEYCLOAK_SESSION_FACTORY).get();
    }

}
