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

import java.util.List;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
public class AcrCondition extends AbstractClientPolicyConditionProvider<AcrCondition.Configuration> {

    public AcrCondition(KeycloakSession session) {
        super(session);
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        @JsonProperty("acr-property")
        protected String acrProperty;

        public String getAcrProperty() {
            return acrProperty;
        }

        public void setAcrProperty(String acrProperty) {
            this.acrProperty = acrProperty;
        }
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public String getProviderId() {
        return AnyClientConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        if (context.getEvent() == ClientPolicyEvent.AUTHORIZATION_REQUEST) {
            AuthorizationRequestContext authorizationRequestContext = ((AuthorizationRequestContext) context);
            if (containsAcr(authorizationRequestContext)) {
                authorizationRequestContext.getAuthenticationSession().setAuthNote(Constants.CLIENT_POLICY_REQUESTED_ACR, configuration.getAcrProperty());
                return ClientPolicyVote.YES;
            }
            else {
                return ClientPolicyVote.NO;
            }
        }
        return ClientPolicyVote.ABSTAIN;
    }

    private boolean containsAcr(AuthorizationRequestContext context) {
        List<String> acrValues = AcrUtils.getAcrValues(context.getAuthorizationEndpointRequest().getClaims(), context.getAuthorizationEndpointRequest().getAcr(), session.getContext().getClient());
        return acrValues != null && !acrValues.isEmpty() && acrValues.contains(configuration.getAcrProperty());
    }

}
