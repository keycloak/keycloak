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

package org.keycloak.services.clientpolicy.executor;

import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.sessions.AuthenticationSessionModel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano</a>
 */
public class AuthenticationFlowSelectorExecutor implements ClientPolicyExecutorProvider<AuthenticationFlowSelectorExecutor.Configuration> {

    private Configuration configuration;

    public AuthenticationFlowSelectorExecutor() {
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("auth-flow-alias")
        protected String authFlowAlias;

        @JsonProperty("auth-flow-loa")
        protected Integer authFlowLoa;

        public String getAuthFlowAlias() {
            return authFlowAlias;
        }

        public void setAuthFlowAlias(String authFlowAlias) {
            this.authFlowAlias = authFlowAlias;
        }

        public Integer getAuthFlowLoa() {
            return authFlowLoa;
        }

        public void setAuthFlowLoa(Integer authFlowLoa) {
            this.authFlowLoa = authFlowLoa;
        }
    }

    @Override
    public String getProviderId() {
        return PKCEEnforcerExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (context.getEvent() == ClientPolicyEvent.AUTHORIZATION_REQUEST) {
            AuthorizationRequestContext authorizationRequestContext = (AuthorizationRequestContext) context;
            executeOnAuthorizationRequest(authorizationRequestContext.getAuthenticationSession());
        }
    }

    private void executeOnAuthorizationRequest(AuthenticationSessionModel authSession) {
        if (configuration.getAuthFlowAlias() != null) {
            authSession.setAuthNote(Constants.REQUESTED_AUTHENTICATION_FLOW, configuration.getAuthFlowAlias());
            // auth flow selected via acr condition
            if (configuration.getAuthFlowLoa() != null) {
                authSession.setAuthNote(Constants.AUTHENTICATION_FLOW_LEVEL_OF_AUTHENTICATION, String.valueOf(configuration.getAuthFlowLoa()));
            }
        }
    }


}
