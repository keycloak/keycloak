/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.condition;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public interface ClientPolicyConditionProviderFactory extends ProviderFactory<ClientPolicyConditionProvider>, ConfiguredProvider, EnvironmentDependentProviderFactory {

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES);
    }

    /**
     * Called before a Client Policy is created or updated.  Allows you to validate the configuration
     *
     * @param session
     * @param realm
     * @param conditionRepresentation
     * @throws ClientPolicyException
     */
    default void validateConfiguration(KeycloakSession session, RealmModel realm, ClientPolicyConditionRepresentation conditionRepresentation) throws ClientPolicyException {
    }
}
