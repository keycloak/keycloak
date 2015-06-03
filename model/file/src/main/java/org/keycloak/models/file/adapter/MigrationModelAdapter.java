package org.keycloak.models.file.adapter;

import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.migration.MigrationModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationModelAdapter implements MigrationModel {
    protected InMemoryModel em;

    public MigrationModelAdapter(InMemoryModel em) {
        this.em = em;
    }

    @Override
    public String getStoredVersion() {
        return em.getModelVersion();
    }

    @Override
    public void setStoredVersion(String version) {
       em.setModelVersion(version);
    }
}
