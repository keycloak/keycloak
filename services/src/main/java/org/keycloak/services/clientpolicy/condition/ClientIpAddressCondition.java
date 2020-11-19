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
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

public class ClientIpAddressCondition implements ClientPolicyConditionProvider {

    private static final Logger logger = Logger.getLogger(ClientIpAddressCondition.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;

    public ClientIpAddressCondition(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case TOKEN_REQUEST:
            case TOKEN_REFRESH:
            case TOKEN_REVOKE:
            case TOKEN_INTROSPECT:
            case USERINFO_REQUEST:
            case LOGOUT_REQUEST:
                if (isIpAddressMatched()) return ClientPolicyVote.YES;
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.ABSTAIN;
        }
    }

    private boolean isIpAddressMatched() {
        String ipAddr = session.getContext().getConnection().getRemoteAddr();

        List<String> expectedIpAddresses = componentModel.getConfig().get(ClientIpAddressConditionFactory.IPADDR);
        if (expectedIpAddresses == null) expectedIpAddresses = Collections.emptyList();

        if (logger.isTraceEnabled()) {
            ClientPolicyLogger.log(logger, "ip address = " + ipAddr);
            expectedIpAddresses.stream().forEach(i -> ClientPolicyLogger.log(logger, "ip address expected = " + i));
        }

        boolean isMatched = expectedIpAddresses.stream().anyMatch(i -> i.equals(ipAddr));
        if (isMatched) {
           ClientPolicyLogger.log(logger, "ip address matched.");
        }  else {
           ClientPolicyLogger.log(logger, "ip address unmatched.");
        }
        return isMatched;
    }

}
