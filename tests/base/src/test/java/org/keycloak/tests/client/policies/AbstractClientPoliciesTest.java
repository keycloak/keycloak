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
package org.keycloak.tests.client.policies;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Assertions;

/**
 *
 * @author rmartinc
 */
public class AbstractClientPoliciesTest {

    protected String generateSuffixedName(String name) {
        return name + "-" + UUID.randomUUID().toString().subSequence(0, 7);
    }

    protected String createClientByAdmin(ManagedRealm realm, String clientName, String protocol, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientName);
        clientRep.setName(clientName);
        clientRep.setProtocol(protocol);
        clientRep.setRedirectUris(Collections.singletonList(realm.getBaseUrl() + "/app/auth"));
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setPostLogoutRedirectUris(Collections.singletonList("+"));
        if (protocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            clientRep.setBearerOnly(Boolean.FALSE);
            clientRep.setPublicClient(Boolean.FALSE);
            clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        } else {
            clientRep.setPublicClient(Boolean.TRUE);
        }
        op.accept(clientRep);
        try (Response resp = realm.admin().clients().create(clientRep)) {
            if (resp.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                String respBody = resp.readEntity(String.class);
                Map<String, String> responseJson = null;
                try {
                    responseJson = JsonSerialization.readValue(respBody, Map.class);
                } catch (IOException e) {
                    Assertions.fail();
                }
                throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR), responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
            }
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
            // registered components will be removed automatically when a test method finishes regardless of its success or failure.
            String cId = ApiUtil.getCreatedId(resp);
            realm.cleanup().add(r -> r.clients().delete(cId));
            return cId;
        }
    }

    protected ClientRepresentation findByClientIdByAdmin(ManagedRealm realm, String clientId) throws ClientPolicyException {
        return realm.admin().clients().findByClientId(clientId).iterator().next();
    }

    protected void updateClientByAdmin(ManagedRealm realm, String cId, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientResource clientResource = realm.admin().clients().get(cId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        op.accept(clientRep);
        try {
            clientResource.update(clientRep);
        } catch (BadRequestException bre) {
            processClientPolicyExceptionByAdmin(bre);
        }
    }

    private void processClientPolicyExceptionByAdmin(BadRequestException bre) throws ClientPolicyException {
        Response resp = bre.getResponse();
        if (resp.getStatus() != Response.Status.BAD_REQUEST.getStatusCode()) {
            resp.close();
            return;
        }

        String respBody = resp.readEntity(String.class);
        Map<String, String> responseJson = null;
        try {
            responseJson = JsonSerialization.readValue(respBody, Map.class);
        } catch (IOException e) {
            Assertions.fail();
        }
        throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR), responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
    }

    protected String createClientDynamically(ManagedRealm realm, ClientRegistration reg, String clientName, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = new OIDCClientRepresentation();
        clientRep.setClientName(clientName);
        clientRep.setClientUri(realm.getBaseUrl());
        clientRep.setRedirectUris(Collections.singletonList(realm.getBaseUrl() + "/app/auth"));
        op.accept(clientRep);

        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        String clientId = response.getClientId();
        realm.cleanup().add(r -> r.clients().delete(clientId));
        return clientId;
    }

    protected void updateClientDynamically(ClientRegistration reg, String clientId, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = reg.oidc().get(clientId);
        op.accept(clientRep);
        OIDCClientRepresentation response = reg.oidc().update(clientRep);
        reg.auth(Auth.token(response));
    }

    protected void setupPolicy(ManagedRealm realm, String executorId, ClientPolicyExecutorConfigurationRepresentation executorConfig,
            String conditionId, ClientPolicyConditionConfigurationRepresentation conditionConfig) throws Exception {
        realm.updateWithCleanup(r -> {
            r.resetClientProfiles()
                    .clientProfile(ClientProfileBuilder.create()
                    .name("executor")
                    .description("executor description")
                    .executor(executorId, executorConfig)
                    .build());
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                    .name("policy")
                    .description("description of policy")
                    .condition(conditionId, conditionConfig)
                    .profile("executor")
                    .build());
            return r;
        });
    }
}
