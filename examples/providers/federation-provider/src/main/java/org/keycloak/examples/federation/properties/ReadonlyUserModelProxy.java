package org.keycloak.examples.federation.properties;

import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * Readonly proxy for a UserModel that prevents passwords from being updated.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ReadonlyUserModelProxy extends UserModelDelegate {

    public ReadonlyUserModelProxy(UserModel delegate) {
        super(delegate);
    }

    @Override
    public void setUsername(String username) {
        throw new IllegalStateException("Username is readonly");
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            throw new IllegalStateException("Passwords are readonly");
        }
        super.updateCredentialDirectly(cred);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            throw new IllegalStateException("Passwords are readonly");
        }
        super.updateCredential(cred);
    }
}
