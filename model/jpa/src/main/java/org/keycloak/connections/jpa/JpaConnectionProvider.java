package org.keycloak.connections.jpa;

import org.keycloak.provider.Provider;

import javax.persistence.EntityManager;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface JpaConnectionProvider extends Provider {

    EntityManager getEntityManager();

}
