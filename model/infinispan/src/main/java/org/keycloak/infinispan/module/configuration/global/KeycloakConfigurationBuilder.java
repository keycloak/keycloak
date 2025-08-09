package org.keycloak.infinispan.module.configuration.global;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.keycloak.models.KeycloakSessionFactory;

public class KeycloakConfigurationBuilder implements Builder<KeycloakConfiguration> {

    private final AttributeSet attributes;

    public KeycloakConfigurationBuilder(GlobalConfigurationBuilder unused) {
        attributes = KeycloakConfiguration.attributeSet();
    }

    @Override
    public KeycloakConfiguration create() {
        return new KeycloakConfiguration(attributes.protect());
    }

    @Override
    public Builder<?> read(KeycloakConfiguration template, Combine combine) {
        attributes.read(template.attributes(), combine);
        return this;
    }

    @Override
    public AttributeSet attributes() {
        return attributes;
    }

    @Override
    public void validate() {

    }

    public KeycloakConfigurationBuilder setKeycloakSessionFactory(KeycloakSessionFactory keycloakSessionFactory) {
        attributes.attribute(KeycloakConfiguration.KEYCLOAK_SESSION_FACTORY).set(keycloakSessionFactory);
        return this;
    }

    /**
     * Configures the interval, in seconds, to check the cluster health (network partitions).
     * <p>
     * A small value may increase the load in the database, as it is used to discovery the cluster members. A zero or
     * negative value disables the check.
     *
     * @param intervalSeconds The interval in seconds. It can be zero or negative.
     * @return {@code this}.
     */
    public KeycloakConfigurationBuilder setClusterHealthInterval(int intervalSeconds) {
        attributes.attribute(KeycloakConfiguration.CLUSTER_HEALTH_INTERVAL).set(intervalSeconds);
        return this;
    }

}
