package org.keycloak.infinispan.module.configuration.global;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.keycloak.models.KeycloakSessionFactory;

@BuiltBy(KeycloakConfigurationBuilder.class)
public class KeycloakConfiguration {

    static final AttributeDefinition<KeycloakSessionFactory> KEYCLOAK_SESSION_FACTORY = AttributeDefinition.builder("keycloak-session-factory", null, KeycloakSessionFactory.class)
            .global(true)
            .autoPersist(false)
            .immutable()
            .build();
    static final AttributeDefinition<Integer> CLUSTER_HEALTH_INTERVAL = AttributeDefinition.builder("cluster-health-interval", -1)
            .global(true)
            .autoPersist(false)
            .immutable()
            .build();

    private final AttributeSet attributes;

    static AttributeSet attributeSet() {
        return new AttributeSet(KeycloakConfiguration.class, KEYCLOAK_SESSION_FACTORY, CLUSTER_HEALTH_INTERVAL);
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

    public int clusterHealthInterval() {
        return attributes.attribute(CLUSTER_HEALTH_INTERVAL).get();
    }

}
