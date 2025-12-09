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

package org.keycloak.services.clientpolicy.condition;

import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.JWTAuthorizationGrantContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author rmartinc
 */
public class IdentityProviderCondition extends AbstractClientPolicyConditionProvider<IdentityProviderCondition.Configuration> {

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        @JsonProperty(IdentityProviderConditionFactory.IDENTITY_PROVIDERS_ALIASES)
        protected List<String> identityProviderAliases;

        public List<String> getIdentityProviderAliases() {
            return identityProviderAliases;
        }

        public void setIdentityProviderAliases(List<String> identityProviderAliases) {
            this.identityProviderAliases = identityProviderAliases;
        }
    }

    public IdentityProviderCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public String getProviderId() {
        return IdentityProviderConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        return switch (context.getEvent()) {
            case JWT_AUTHORIZATION_GRANT -> isIdentityProvider(((JWTAuthorizationGrantContext) context).getIdentityProvider().getAlias())
                ? ClientPolicyVote.YES
                : ClientPolicyVote.NO;
            default ->  ClientPolicyVote.ABSTAIN;
        };
    }

    private boolean isIdentityProvider(String identityProviderAlias) throws ClientPolicyException {
        return configuration.identityProviderAliases.contains(identityProviderAlias);
    }
}
