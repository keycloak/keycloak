package org.keycloak.federation.ldap;

import org.keycloak.models.ModelReadOnlyException;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ReadonlyLDAPUserModelDelegate extends UserModelDelegate implements UserModel {

    protected LDAPFederationProvider provider;

    public ReadonlyLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }

    @Override
    public void setUsername(String username) {
        throw new ModelReadOnlyException("Federated storage is not writable");
    }

    @Override
    public void setLastName(String lastName) {
        throw new ModelReadOnlyException("Federated storage is not writable");
    }

    @Override
    public void setFirstName(String first) {
        throw new ModelReadOnlyException("Federated storage is not writable");
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (provider.getSupportedCredentialTypes(delegate).contains(cred.getType())) {
            throw new ModelReadOnlyException("Federated storage is not writable");
        }
        delegate.updateCredential(cred);
    }

    @Override
    public void setEmail(String email) {
        throw new ModelReadOnlyException("Federated storage is not writable");
    }

}
