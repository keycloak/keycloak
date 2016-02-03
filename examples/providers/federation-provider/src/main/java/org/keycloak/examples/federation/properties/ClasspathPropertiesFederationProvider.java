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

import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClasspathPropertiesFederationProvider extends BasePropertiesFederationProvider {

    public ClasspathPropertiesFederationProvider(KeycloakSession session, UserFederationProviderModel model, Properties properties) {
        super(session, model, properties);
    }

    /**
     * Keycloak will call this method if it finds an imported UserModel.  Here we proxy the UserModel with
     * a Readonly proxy which will barf if password is updated.
     *
     * @param local
     * @return
     */
    @Override
    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        if (isValid(realm, local)) {
            return new ReadonlyUserModelProxy(local);
        } else {
            return null;
        }
    }

    /**
     * The properties file is readonly so don't suppport registration.
     *
     * @return
     */
    @Override
    public boolean synchronizeRegistrations() {
        return false;
    }

    /**
     * The properties file is readonly so don't suppport registration.
     *
     * @return
     */
    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Registration not supported");
    }

    /**
     * The properties file is readonly so don't removing a user
     *
     * @return
     */
    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Remove not supported");
    }



}
