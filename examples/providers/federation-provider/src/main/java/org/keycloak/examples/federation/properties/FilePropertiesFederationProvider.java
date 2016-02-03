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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FilePropertiesFederationProvider extends BasePropertiesFederationProvider {

    public FilePropertiesFederationProvider(KeycloakSession session, Properties properties, UserFederationProviderModel model) {
        super(session, model, properties);
    }

    /**
     * Keycloak will call this method if it finds an imported UserModel.  Here we proxy the UserModel with
     * a Writable proxy which will synchronize updates to username and password back to the properties file
     *
     * @param local
     * @return
     */
    @Override
    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        if (isValid(realm, local)) {
            return new WritableUserModelProxy(local, this);
        } else {
            return null;
        }
    }

    /**
     * Adding new users is supported
     *
     * @return
     */
    @Override
    public boolean synchronizeRegistrations() {
        return true;
    }

    public void save() {
        String path = getModel().getConfig().get("path");
        try {
            FileOutputStream fos = new FileOutputStream(path);
            properties.store(fos, "");
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update the properties file with the new user.
     *
     * @param realm
     * @param user
     * @return
     */
    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        synchronized (properties) {
            properties.setProperty(user.getUsername(), "");
            save();
        }
        return validateAndProxy(realm, user);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        synchronized (properties) {
            if (properties.remove(user.getUsername()) == null) return false;
            save();
            return true;
        }
    }



}
