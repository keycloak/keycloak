package org.keycloak.examples.federation.properties;

import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WritableUserModelProxy extends UserModelDelegate {
    protected FilePropertiesFederationProvider provider;

    public WritableUserModelProxy(UserModel delegate, FilePropertiesFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }

    @Override
    public void setUsername(String username) {
        if (delegate.getUsername().equals(username)) return;
        delegate.setUsername(username);
        Properties properties = provider.getProperties();
        synchronized (properties) {
            if (properties.containsKey(username)) {
                throw new IllegalStateException("Can't change username to existing user");
            }
            String password = (String) properties.remove(username);
            if (password == null) {
                throw new IllegalStateException("User doesn't exist");
            }
            properties.setProperty(username, password);
            provider.save();
        }

    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            throw new IllegalStateException("Shouldn't be using this method");
        }
        super.updateCredentialDirectly(cred);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {

        } else {
            super.updateCredential(cred);
            synchronized (provider.getProperties()) {
                if (!provider.getProperties().containsKey(delegate.getUsername())) {
                    throw new IllegalStateException("no user of that in properties file");
                }
                provider.getProperties().put(delegate.getUsername(), cred.getValue());
                provider.save();
            }
        }
    }
}
