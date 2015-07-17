package org.keycloak.models.jpa;

import org.keycloak.migration.MigrationModel;
import org.keycloak.models.jpa.entities.MigrationModelEntity;
import org.keycloak.util.Time;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationModelAdapter implements MigrationModel {
    protected EntityManager em;

    public MigrationModelAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public String getStoredVersion() {
        MigrationModelEntity entity = em.find(MigrationModelEntity.class, MigrationModelEntity.SINGLETON_ID);
        if (entity == null) return null;
        return entity.getVersion();
    }

    @Override
    public void setStoredVersion(String version) {
        MigrationModelEntity entity = em.find(MigrationModelEntity.class, MigrationModelEntity.SINGLETON_ID);
        if (entity == null) {
            entity = new MigrationModelEntity();
            entity.setId(MigrationModelEntity.SINGLETON_ID);
            entity.setVersion(version);
            em.persist(entity);
        } else {
            entity.setVersion(version);
            em.flush();
        }
    }
}
