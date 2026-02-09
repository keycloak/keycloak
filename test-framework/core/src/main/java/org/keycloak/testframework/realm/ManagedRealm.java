package org.keycloak.testframework.realm;

import java.util.List;

import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.injection.ManagedTestResource;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;

public class ManagedRealm extends ManagedTestResource {

    private final String baseUrl;
    private final RealmRepresentation createdRepresentation;
    private final RealmResource realmResource;
    private String realmId;

    private ManagedRealmCleanup cleanup;

    public ManagedRealm(String baseUrl, RealmRepresentation createdRepresentation, RealmResource realmResource) {
        this.baseUrl = baseUrl;
        this.createdRepresentation = createdRepresentation;
        this.realmResource = realmResource;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getId() {
        if (realmId == null && createdRepresentation.getId() != null) {
            realmId = createdRepresentation.getId();
        } else {
            realmId = admin().toRepresentation().getId();
        }
        return realmId;
    }

    public String getName() {
        return createdRepresentation.getRealm();
    }

    public RealmResource admin() {
        return realmResource;
    }

    public RealmRepresentation getCreatedRepresentation() {
        return createdRepresentation;
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

    public void addUser(UserConfigBuilder user) {
        UserRepresentation rep = user.build();
        String id = ApiUtil.getCreatedId(realmResource.users().create(rep));
        cleanup().add(r -> r.users().get(id).remove());
    }

    public void updateUser(String username, UserConfigBuilder.UserUpdate update) {
        List<UserRepresentation> result = realmResource.users().search(username);
        Assertions.assertEquals(1, result.size());

        UserRepresentation original = result.get(0);
        UserRepresentation updated = RepresentationUtils.clone(original);
        update.update(updated);
        realmResource.users().get(original.getId()).update(updated);

        cleanup().add(r -> r.users().get(original.getId()).update(original));
    }

    public void updateIdentityProvider(String alias, IdentityProviderUpdate update) {
        IdentityProviderResource resource = realmResource.identityProviders().get(alias);

        IdentityProviderRepresentation original = resource.toRepresentation();
        IdentityProviderRepresentation updated = RepresentationUtils.clone(original);
        update.update(updated);
        resource.update(updated);

        cleanup().add(r -> r.identityProviders().get(alias).update(original));
    }

    public void updateComponent(String id, ComponentUpdate update) {
        ComponentResource componentResource = realmResource.components().component(id);

        ComponentRepresentation original = componentResource.toRepresentation();
        ComponentRepresentation updated = RepresentationUtils.clone(original);
        update.update(updated);
        componentResource.update(updated);

        cleanup().add(r -> r.components().component(id).update(original));
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

    public interface IdentityProviderUpdate {

        void update(IdentityProviderRepresentation rep);

    }

    public interface ComponentUpdate {

        void update(ComponentRepresentation rep);

    }



}
