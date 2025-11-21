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

import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:ggrazian@redhat.com">Giuseppe Graziano/a>
 */
public class GrantTypeCondition extends AbstractClientPolicyConditionProvider<GrantTypeCondition.Configuration> {

    private static final Logger logger = Logger.getLogger(GrantTypeCondition.class);

    public GrantTypeCondition(KeycloakSession session) {
        super(session);
    }

    @Override
    public Class<Configuration> getConditionConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyConditionConfigurationRepresentation {

        @JsonProperty("grant_types")
        protected List<String> grantTypes;

        public List<String> getGrantTypes() {
            return grantTypes;
        }

        public void setGrantTypes(List<String> grantTypes) {
            this.grantTypes = grantTypes;
        }
    }

    @Override
    public String getProviderId() {
        return ClientScopesConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {

        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
                if (isGrantMatching((AuthorizationRequestContext)context)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case TOKEN_REFRESH:
                if (isGrantMatching(OAuth2Constants.REFRESH_TOKEN)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case RESOURCE_OWNER_PASSWORD_CREDENTIALS_REQUEST:
                if (isGrantMatching(OAuth2Constants.PASSWORD)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case SERVICE_ACCOUNT_TOKEN_REQUEST:
                if (isGrantMatching(OAuth2Constants.CLIENT_CREDENTIALS)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case TOKEN_EXCHANGE_REQUEST:
                if (isGrantMatching(OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            case DEVICE_TOKEN_REQUEST:
                if (isGrantMatching(OAuth2Constants.DEVICE_CODE_GRANT_TYPE)) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isGrantMatching(AuthorizationRequestContext request) {
        if (request == null) return false;
        try {
            OIDCResponseType parsedResponseType = OIDCResponseType.parse(request.getAuthorizationEndpointRequest().getResponseType());
            if (parsedResponseType.hasResponseType(OIDCResponseType.CODE)) {
                return isGrantMatching(OAuth2Constants.AUTHORIZATION_CODE);
            }
            else if (parsedResponseType.isImplicitFlow()) {
                return isGrantMatching(OAuth2Constants.IMPLICIT);
            }
            else if (parsedResponseType.isImplicitOrHybridFlow()) {
                return isGrantMatching(OAuth2Constants.AUTHORIZATION_CODE) || isGrantMatching(OAuth2Constants.IMPLICIT);
            }
            else {
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isGrantMatching(String grantType) {
        return configuration.getGrantTypes() != null && configuration.getGrantTypes().contains(grantType);
    }
}
