package org.keycloak.test.framework.realm;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.injection.ManagedTestResource;

public class ManagedRealm extends ManagedTestResource {

    private final String baseUrl;
    private final RealmRepresentation createdRepresentation;
    private final RealmResource realmResource;

    private ManagedRealmCleanup cleanup;

    public ManagedRealm(String baseUrl, RealmRepresentation createdRepresentation, RealmResource realmResource) {
        this.baseUrl = baseUrl;
        this.createdRepresentation = createdRepresentation;
        this.realmResource = realmResource;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getName() {
        return createdRepresentation.getRealm();
    }

    public RealmResource admin() {
        return realmResource;
    }

    public void updateWithCleanup(RealmUpdate... updates) {
        RealmRepresentation rep = admin().toRepresentation();
        cleanup().resetToOriginalRepresentation(rep);

        RealmConfigBuilder configBuilder = RealmConfigBuilder.update(rep);
        for (RealmUpdate update : updates) {
            configBuilder = update.update(configBuilder);
        }

        admin().update(configBuilder.build());
    }

    public ManagedRealmCleanup cleanup() {
        if (cleanup == null) {
            cleanup = new ManagedRealmCleanup();
        }
        return cleanup;
    }

    @Override
    public void runCleanup() {
        if (cleanup != null) {
            cleanup.runCleanupTasks(realmResource);
            cleanup = null;
        }
    }

    public interface RealmUpdate {

        RealmConfigBuilder update(RealmConfigBuilder realm);

    }

}
