package org.keycloak.testframework.realm;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.injection.ManagedTestResource;

import java.util.Optional;

public class ManagedUser extends ManagedTestResource {

    private final UserRepresentation createdRepresentation;
    private final UserResource userResource;

    private ManagedUserCleanup cleanup;

    public ManagedUser(UserRepresentation createdRepresentation, UserResource userResource) {
        this.createdRepresentation = createdRepresentation;
        this.userResource = userResource;
    }

    public String getId() {
        return createdRepresentation.getId();
    }

    public String getUsername() {
        return createdRepresentation.getUsername();
    }

    public String getPassword() {
        return getPassword(createdRepresentation);
    }

    public UserResource admin() {
        return userResource;
    }

    public static String getPassword(UserRepresentation userRepresentation) {
        Optional<CredentialRepresentation> password = userRepresentation.getCredentials().stream().filter(c -> c.getType().equals(CredentialRepresentation.PASSWORD)).findFirst();
        return password.map(CredentialRepresentation::getValue).orElse(null);
    }

    public void updateWithCleanup(UserUpdate... updates) {
        UserRepresentation rep = admin().toRepresentation();
        cleanup().resetToOriginalRepresentation(rep);

        UserConfigBuilder configBuilder = UserConfigBuilder.update(rep);
        for (ManagedUser.UserUpdate update : updates) {
            configBuilder = update.update(configBuilder);
        }

        admin().update(configBuilder.build());
    }

    public ManagedUserCleanup cleanup() {
        if (cleanup == null) {
            cleanup = new ManagedUserCleanup();
        }
        return cleanup;
    }

    @Override
    public void runCleanup() {
        if (cleanup != null) {
            cleanup.runCleanupTasks(userResource);
            cleanup = null;
        }
    }

    public interface UserUpdate {

        UserConfigBuilder update(UserConfigBuilder user);

    }
}
