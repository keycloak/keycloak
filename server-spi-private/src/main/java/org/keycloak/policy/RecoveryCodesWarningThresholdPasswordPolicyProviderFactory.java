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
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 *
 * @deprecated Please use rather the recovery codes required action to configure warning threshold for recovery codes. This password policy may be removed in the future versions.
 */
@Deprecated
public class RecoveryCodesWarningThresholdPasswordPolicyProviderFactory implements PasswordPolicyProviderFactory, PasswordPolicyProvider, EnvironmentDependentProviderFactory {

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
        return PasswordPolicy.RECOVERY_CODES_WARNING_THRESHOLD_ID;
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
        return "Recovery Codes Warning Threshold";
    }

    @Override
    public String getConfigType() {
        return PasswordPolicyProvider.INT_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return String.valueOf(PasswordPolicy.RECOVERY_CODES_WARNING_THRESHOLD_DEFAULT);
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public Object parseConfig(String value) {
        return parseInteger(value, PasswordPolicy.RECOVERY_CODES_WARNING_THRESHOLD_DEFAULT);
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RECOVERY_CODES);
    }
}
