package org.keycloak.ssf.transmitter.outbox.jpa;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * Keycloak picks up this factory at startup via the
 * {@code org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory}
 * ServiceLoader file. The resulting provider adds the SSF push outbox
 * entity to the JPA entity manager and wires its Liquibase changelog
 * into schema migration.
 *
 * <p>The factory is gated on {@link Profile.Feature#SSF} — without the
 * SSF feature flag the SSF subsystem is otherwise inactive, so there's
 * no reason to run its schema changes.
 */
public class SsfJpaEntityProviderFactory
        implements JpaEntityProviderFactory, EnvironmentDependentProviderFactory {

    /**
     * Names the Liquibase tracking table for this provider's changelog.
     * Liquibase derives the tracking table name from this id, so changing
     * it on an upgraded installation would orphan previously-applied
     * changesets — keep it stable.
     */
    public static final String ID = "ssf-entity-provider";

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new SsfJpaEntityProvider();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SSF);
    }
}
