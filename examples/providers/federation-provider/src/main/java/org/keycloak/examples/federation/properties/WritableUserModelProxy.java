/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.examples.federation.properties;

import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

import java.util.Properties;

/**
 * Proxy that will synchronize password updates to the properties file.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WritableUserModelProxy extends UserModelDelegate {
    protected FilePropertiesFederationProvider provider;

    public WritableUserModelProxy(UserModel delegate, FilePropertiesFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }


    /**
     * Updates the properties file if the username changes.  If you have a more complex user storage, you can
     * override other methods on UserModel to synchronize updates back to your external storage.
     *
     * @param username
     */
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
            synchronized (provider.getProperties()) {
                if (!provider.getProperties().containsKey(delegate.getUsername())) {
                    throw new IllegalStateException("no user of that in properties file");
                }
                provider.getProperties().setProperty(delegate.getUsername(), cred.getValue());
                provider.save();
            }
        } else {
            super.updateCredential(cred);
        }
    }
}
