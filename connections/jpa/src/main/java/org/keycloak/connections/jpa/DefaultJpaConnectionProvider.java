package org.keycloak.connections.jpa;

import javax.persistence.EntityManager;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultJpaConnectionProvider implements JpaConnectionProvider {

    private final EntityManager em;

    public DefaultJpaConnectionProvider(EntityManager em) {
        this.em = em;
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    @Override
    public void close() {
        em.close();
    }

}
