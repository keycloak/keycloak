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

package org.keycloak.policy;

import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HashAlgorithmPasswordPolicyProviderFactory implements PasswordPolicyProviderFactory, PasswordPolicyProvider {

    private KeycloakSession session;

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PasswordPolicy.HASH_ALGORITHM_ID;
    }

    @Override
    public PolicyError validate(RealmModel realm, UserModel user, String password) {
        return null;
    }

    @Override
    public PolicyError validate(String user, String password) {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Hashing Algorithm";
    }

    @Override
    public String getConfigType() {
        return PasswordPolicyProvider.STRING_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return PasswordPolicy.HASH_ALGORITHM_DEFAULT;
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public Object parseConfig(String value) {
        String providerId = value != null && value.length() > 0 ? value : PasswordPolicy.HASH_ALGORITHM_DEFAULT;
        PasswordHashProvider provider = session.getProvider(PasswordHashProvider.class, providerId);
        if (provider == null) {
            throw new PasswordPolicyConfigException("Password hashing provider not found");
        }
        return providerId;
    }

}
