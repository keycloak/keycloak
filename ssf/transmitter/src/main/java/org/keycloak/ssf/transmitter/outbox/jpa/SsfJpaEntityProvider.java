package org.keycloak.ssf.transmitter.outbox.jpa;

import java.util.List;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.ssf.transmitter.outbox.SsfPendingEventEntity;

/**
 * Contributes the SSF push outbox entity and its Liquibase changelog
 * to Keycloak's JPA layer. Keycloak discovers this provider via the
 * {@code JpaEntityProviderFactory} SPI at server startup and adds the
 * entity class to its entity manager, runs the changelog as part of
 * database initialization, and tracks it in a dedicated changelog
 * table named after {@link SsfJpaEntityProviderFactory#ID}.
 */
public class SsfJpaEntityProvider implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return List.of(SsfPendingEventEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        // Master changelog that <include>s per-version files. Mirrors
        // the model/jpa convention (META-INF/jpa-changelog-master.xml).
        return "META-INF/ssf-changelog-master.xml";
    }

    @Override
    public String getFactoryId() {
        return SsfJpaEntityProviderFactory.ID;
    }

    @Override
    public void close() {
        // NOOP
    }
}
