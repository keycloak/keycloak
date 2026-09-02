/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Specifies the maximum age of an authentication with which a password may be changed without re-authentication.
 */
public class MaxAuthAgePasswordPolicyProviderFactory implements PasswordPolicyProvider, PasswordPolicyProviderFactory {

    public static final int DEFAULT_MAX_AUTH_AGE = Constants.KC_ACTION_MAX_AGE;

    @Override
    public PasswordPolicyProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return PasswordPolicy.MAX_AUTH_AGE_ID;
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
    public Object parseConfig(String value) {
        return parseInteger(value, -1);
    }

    @Override
    public String getDisplayName() {
        return "Maximum Authentication Age";
    }

    @Override
    public String getConfigType() {
        return PasswordPolicyProvider.INT_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return String.valueOf(DEFAULT_MAX_AUTH_AGE);
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public void close() {
    }

}
