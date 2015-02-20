package org.keycloak.federation.kerberos;

import org.keycloak.models.ModelReadOnlyException;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ReadOnlyKerberosUserModelDelegate extends UserModelDelegate {

    protected KerberosFederationProvider provider;

    public ReadOnlyKerberosUserModelDelegate(UserModel delegate, KerberosFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (provider.getSupportedCredentialTypes(delegate).contains(cred.getType())) {
            throw new ModelReadOnlyException("Can't change password in Keycloak database. Change password with your Kerberos server");
        }

        delegate.updateCredential(cred);
    }
}
