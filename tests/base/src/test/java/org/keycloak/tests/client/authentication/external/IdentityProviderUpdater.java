package org.keycloak.tests.client.authentication.external;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RepresentationUtils;

public class IdentityProviderUpdater {

    public static void updateWithRollback(ManagedRealm realm, String alias, IdentityProviderUpdate update) {
        IdentityProviderResource resource = realm.admin().identityProviders().get(alias);

        IdentityProviderRepresentation original = resource.toRepresentation();
        IdentityProviderRepresentation updated = RepresentationUtils.clone(original);
        update.update(updated);
        resource.update(updated);

        realm.cleanup().add(r -> r.identityProviders().get(alias).update(original));
    }

    public interface IdentityProviderUpdate {

        void update(IdentityProviderRepresentation rep);

    }

}
