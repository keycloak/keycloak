/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientRolesExecutor implements ClientPolicyExecutorProvider<ClientRolesExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(ClientRolesExecutor.class);

    private final KeycloakSession session;
    private Configuration configuration;

    public ClientRolesExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        if (config == null) {
            // Fallback for the case that null configuration is passed as an argument
            this.configuration = JsonSerialization.mapper.convertValue(new ClientPolicyExecutorConfigurationRepresentation(), getExecutorConfigurationClass());
        } else {
            this.configuration = config;
        }
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        protected List<String> roles;

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

    @Override
    public String getProviderId() {
        return ClientRolesExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case AUTHORIZATION_REQUEST:
        case TOKEN_REQUEST:
        case SERVICE_ACCOUNT_TOKEN_REQUEST:
        case TOKEN_REFRESH:
        case TOKEN_REVOKE:
        case TOKEN_INTROSPECT:
        case USERINFO_REQUEST:
        case LOGOUT_REQUEST:
        case BACKCHANNEL_AUTHENTICATION_REQUEST:
        case BACKCHANNEL_TOKEN_REQUEST:
        case PUSHED_AUTHORIZATION_REQUEST:
            if (isRolesMatched(session.getContext().getClient())) return;
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "no adequate client role");
        default:
            return;
        }
    }

    private boolean isRolesMatched(ClientModel client) {
        if (client == null) return false;

        Set<String> rolesForMatching = getRolesForMatching();
        if (rolesForMatching == null) return false;

        // client.getRolesStream() never returns null according to {@link RoleProvider.getClientRolesStream}
        Set<String> clientRoles = client.getRolesStream().map(RoleModel::getName).collect(Collectors.toSet());

        if (logger.isTraceEnabled()) {
            clientRoles.forEach(i -> logger.tracev("client role assigned = {0}", i));
            rolesForMatching.forEach(i -> logger.tracev("client role for matching = {0}", i));
        }

        return rolesForMatching.removeAll(clientRoles);  // may change rolesForMatching so that it has needed to be instantiated.
    }

    private Set<String> getRolesForMatching() {
        if (configuration.getRoles() == null) return null;
        return new HashSet<>(configuration.getRoles());
    }

}
