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

package org.keycloak.testsuite.services.clientpolicy.executor;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.AbstractAugumentingClientRegistrationPolicyExecutor;

public class TestClientAuthenticationExecutor extends AbstractAugumentingClientRegistrationPolicyExecutor {

    private static final Logger logger = Logger.getLogger(TestClientAuthenticationExecutor.class);

    public TestClientAuthenticationExecutor(KeycloakSession session, ComponentModel componentModel) {
        super(session, componentModel);
    }

    protected void augment(ClientRepresentation rep) {
        if (Boolean.valueOf(componentModel.getConfig().getFirst(AbstractAugumentingClientRegistrationPolicyExecutor.IS_AUGMENT)))
            rep.setClientAuthenticatorType(enforcedClientAuthenticatorType());
    }

    protected void validate(ClientRepresentation rep) throws ClientPolicyException {
        verifyClientAuthenticationMethod(rep.getClientAuthenticatorType());
    }

    private String enforcedClientAuthenticatorType() {
        return componentModel.getConfig().getFirst(TestClientAuthenticationExecutorFactory.CLIENT_AUTHNS_AUGMENT);
    }

    private void verifyClientAuthenticationMethod(String clientAuthenticatorType) throws ClientPolicyException {
        List<String> acceptableClientAuthn = componentModel.getConfig().getList(TestClientAuthenticationExecutorFactory.CLIENT_AUTHNS);
        if (acceptableClientAuthn != null && acceptableClientAuthn.stream().anyMatch(i->i.equals(clientAuthenticatorType))) return;
        throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: token_endpoint_auth_method");
    }

}
