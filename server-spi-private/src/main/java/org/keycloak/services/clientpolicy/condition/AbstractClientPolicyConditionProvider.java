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
 *
 */

package org.keycloak.services.clientpolicy.condition;

import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractClientPolicyConditionProvider<CONFIG extends ClientPolicyConditionConfigurationRepresentation> implements ClientPolicyConditionProvider<CONFIG> {

    protected final KeycloakSession session;
    protected CONFIG configuration;

    public AbstractClientPolicyConditionProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(CONFIG config) {
        if (config == null) {
            // Fallback for the case that null configuration is passed as an argument
            this.configuration = JsonSerialization.mapper.convertValue(new ClientPolicyConditionConfigurationRepresentation(), getConditionConfigurationClass());
        } else {
            this.configuration = config;
        }
    }

    public boolean isNegativeLogic() throws ClientPolicyException {
        if (configuration == null) {
            throw new ClientPolicyException("Not allowed to call this when configuration is not set");
        }
        return Optional.ofNullable(this.configuration.isNegativeLogic()).orElse(Boolean.FALSE).booleanValue();
    }
}
