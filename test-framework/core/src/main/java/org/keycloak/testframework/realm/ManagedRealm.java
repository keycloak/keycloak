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

/**
 * Utilities to work with managed realms
 */
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

    /**
     * The base URL of the realm (for example <code>http://localhost:8080/realms/myrealm</code>)
     *
     * @return the realm base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * The UUID of the realm
     *
     * @return realm UUID
     */
    public String getId() {
        if (realmId == null && createdRepresentation.getId() != null) {
            realmId = createdRepresentation.getId();
        } else {
            realmId = admin().toRepresentation().getId();
        }
        return realmId;
    }

    /**
     * The name of the realm
     *
     * @return realm name
     */
    public String getName() {
        return createdRepresentation.getRealm();
    }

    /**
     * Admin realm resource for the realm to view or update the configuration of the realm. Updates should in general
     * not be done directly through the realm resource as it will leave the realm in an unexpected state for sub-sequent
     * tests
     *
     * @return realm resource
     */
    public RealmResource admin() {
        return realmResource;
    }

    /**
     * The representation used to create the realm
     *
     * @return realm representation
     */
    public RealmRepresentation getCreatedRepresentation() {
        return createdRepresentation;
    }

    /**
     * Update the realm within a test with automatic reset to the original configuration after the test has completed
     *
     * @param updates the updates to the realm
     */
    public void updateWithCleanup(RealmUpdate... updates) {
        RealmRepresentation rep = admin().toRepresentation();
        cleanup().resetToOriginalRepresentation(rep);

        RealmConfigBuilder configBuilder = RealmConfigBuilder.update(rep);
        for (RealmUpdate update : updates) {
            configBuilder = update.update(configBuilder);
        }

        admin().update(configBuilder.build());
    }

    /**
     * Add a user to the realm, which is automatically removed once the test is completed
     *
     * @param user the user to add
     */
    public void addUser(UserConfigBuilder user) {
        UserRepresentation rep = user.build();
        String id = ApiUtil.getCreatedId(realmResource.users().create(rep));
        cleanup().add(r -> r.users().get(id).remove());
    }

    /**
     * Update a user within the realm, which is automatically reset once the test is completed
     *
     * @param username the username of the user to update
     * @param update the update to perform on the user
     */
    public void updateUser(String username, UserConfigBuilder.UserUpdate update) {
        List<UserRepresentation> result = realmResource.users().search(username);
        Assertions.assertEquals(1, result.size());

        UserRepresentation original = result.get(0);
        UserRepresentation updated = RepresentationUtils.clone(original);
        update.update(updated);
        realmResource.users().get(original.getId()).update(updated);

        cleanup().add(r -> r.users().get(original.getId()).update(original));
    }

    /**
     * Update an identity provider within the realm, which is automatically reset once the test is completed
     *
     * @param alias the alias of the identity provider to update
     * @param update the update to perform on the identity provider
     */
    public void updateIdentityProvider(String alias, IdentityProviderUpdate update) {
        IdentityProviderResource resource = realmResource.identityProviders().get(alias);

        IdentityProviderRepresentation original = resource.toRepresentation();
        IdentityProviderRepresentation updated = RepresentationUtils.clone(original);
        update.update(updated);
        resource.update(updated);

        cleanup().add(r -> r.identityProviders().get(alias).update(original));
    }

    /**
     * Update a component within the realm, which is automatically reset once the test is completed
     *
     * @param id the id of the component to update
     * @param update the update to perform on the component
     */
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
