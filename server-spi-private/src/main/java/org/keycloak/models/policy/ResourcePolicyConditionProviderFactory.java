/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.policy;

import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface ResourcePolicyConditionProviderFactory<P extends ResourcePolicyConditionProvider> extends ProviderFactory<P>, EnvironmentDependentProviderFactory {

    P create(KeycloakSession session, Map<String, List<String>> config);

    @Override
    default P create(KeycloakSession session) {
        throw new IllegalStateException("Use create(KeycloakSession session, MultivaluedHashMap<String, String> config) instead.");
    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RESOURCE_LIFECYCLE);
    }
}
