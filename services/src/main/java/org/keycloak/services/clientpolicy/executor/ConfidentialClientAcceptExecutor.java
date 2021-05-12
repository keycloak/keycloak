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

import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ConfidentialClientAcceptExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    protected final KeycloakSession session;

    public ConfidentialClientAcceptExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return ConfidentialClientAcceptExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
            case TOKEN_REQUEST:
            	checkIsConfidentialClient();
                return;
            default:
                return;
        }
    }

    private void checkIsConfidentialClient() throws ClientPolicyException {
        ClientModel client = session.getContext().getClient();
        if (client == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "invalid client access type");
        }
        if (client.isPublicClient()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "invalid client access type");
        }
        if (client.isBearerOnly()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "invalid client access type");
        }
    }
}
