/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz;

import org.junit.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.authorization.client.resource.PolicyResource;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.UmaPermissionRepresentation;

import java.io.IOException;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthzClientBackChannelTest extends AbstractResourceServerTest {

    @Override
    protected AuthzClient getAuthzClient() {
        try {
            return AuthzClient.create(httpsAwareConfigurationStream(getClass().getResourceAsStream("/authorization-test/keycloak-backchannel.json")));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to create authz client", cause);
        }
    }

    @Test
    public void tokenIntrospectionEndpoint() {
        AuthzClient client = getAuthzClient();
        ProtectionResource protection = client.protection();
        AccessTokenResponse tokenResponse = client.obtainAccessToken();

        TokenIntrospectionResponse response = protection.introspectRequestingPartyToken(tokenResponse.getToken());

        assertNotNull(response);
    }

    @Test
    public void resourceRegistrationEndpoint() {
        AuthzClient client = getAuthzClient();
        ProtectedResource protectionResource = client.protection().resource();

        String[] protectedResources = protectionResource.findAll();

        assertEquals(0, protectedResources.length);
    }

    @Test
    public void permissionEndpoint() {
        AuthzClient client = getAuthzClient();
        PermissionResource permissionResource = client.protection().permission();
        List<PermissionTicketRepresentation> tickets = permissionResource.findByScope("not-existent");

        assertTrue(tickets.isEmpty());
    }

    @Test
    public void policyEndpoint() throws Exception {
        ResourceRepresentation resource = addResource("resource-backchannel");

        AuthzClient client = getAuthzClient();
        PolicyResource policyResource = client.protection().policy(resource.getId());

        List<UmaPermissionRepresentation> permissions = policyResource.find("not-existent", null, null, null);

        assertTrue(permissions.isEmpty());
    }

}
