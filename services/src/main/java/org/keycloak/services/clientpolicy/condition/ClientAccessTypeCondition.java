/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

public class ClientAccessTypeCondition extends AbstractClientPolicyConditionProvider {

    private static final Logger logger = Logger.getLogger(ClientAccessTypeCondition.class);

    public ClientAccessTypeCondition(KeycloakSession session, ComponentModel componentModel) {
        super(session, componentModel);
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case AUTHORIZATION_REQUEST:
            case TOKEN_REQUEST:
            case TOKEN_REFRESH:
            case TOKEN_REVOKE:
            case TOKEN_INTROSPECT:
            case USERINFO_REQUEST:
            case LOGOUT_REQUEST:
                if (isClientAccessTypeMatched()) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    private String getClientAccessType() {
        ClientModel client = session.getContext().getClient();
        if (client == null) return null;

        if (client.isPublicClient()) return ClientAccessTypeConditionFactory.TYPE_PUBLIC;
        if (client.isBearerOnly()) return ClientAccessTypeConditionFactory.TYPE_BEARERONLY;
        else return ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL;
    }

    private boolean isClientAccessTypeMatched() {
        final String accessType = getClientAccessType();

        List<String> expectedAccessTypes = componentModel.getConfig().get(ClientAccessTypeConditionFactory.TYPE);
        if (expectedAccessTypes == null) expectedAccessTypes = Collections.emptyList();

        if (logger.isTraceEnabled()) {
            ClientPolicyLogger.log(logger, "client access type = " + accessType);
            expectedAccessTypes.stream().forEach(i -> ClientPolicyLogger.log(logger, "client access type expected = " + i));
        }

        boolean isMatched = expectedAccessTypes.stream().anyMatch(i -> i.equals(accessType));
        if (isMatched) {
            ClientPolicyLogger.log(logger, "client access type matched.");
        } else {
            ClientPolicyLogger.log(logger, "client access type unmatched.");
        }
        return isMatched;
    }

}
