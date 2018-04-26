package org.keycloak.performance.dataset.idm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.performance.dataset.NestedEntity;
import org.keycloak.representations.idm.CredentialRepresentation;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.performance.dataset.Updatable;

/**
 *
 * @author tkyjovsk
 */
public class Credential extends NestedEntity<User, CredentialRepresentation>
        implements Updatable<CredentialRepresentation> {

    public Credential(User user, int index) {
        super(user, index);
    }

    @Override
    public CredentialRepresentation newRepresentation() {
        return new CredentialRepresentation();
    }

    @Override
    public String toString() {
        return getRepresentation().getType();
    }

    public User getUser() {
        return getParentEntity();
    }

    @Override
    public void update(Keycloak adminClient) {
        if (getRepresentation().getType().equals(PASSWORD)) {
            resource(adminClient).resetPassword(getRepresentation());
        } else {
            logger().warn("Cannot reset password. Non-password credetial type.");
        }
    }

    public UserResource resource(Keycloak adminClient) {
        return getUser().resource(adminClient);
    }

    @Override
    public void delete(Keycloak adminClient) {
        throw new UnsupportedOperationException();
    }

}
