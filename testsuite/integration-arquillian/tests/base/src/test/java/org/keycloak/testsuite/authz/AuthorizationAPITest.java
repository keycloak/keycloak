/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationAPITest extends AbstractAuthzTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("authz-test")
                .roles(RolesBuilder.create().realmRole(RoleBuilder.create().name("uma_authorization").build()))
                .user(UserBuilder.create().username("marta").password("password").addRoles("uma_authorization"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .redirectUris("http://localhost/resource-server-test")
                    .defaultRoles("uma_protection")
                    .directAccessGrants())
                .client(ClientBuilder.create().clientId("test-client")
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .redirectUris("http://localhost/test-client")
                    .directAccessGrants())
                .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();
        ResourceRepresentation resource = new ResourceRepresentation("Resource A");

        Response response = authorization.resources().create(resource);
        response.close();

        JSPolicyRepresentation policy = new JSPolicyRepresentation();

        policy.setName("Default Policy");
        policy.setCode("$evaluation.grant();");

        response = authorization.policies().js().create(policy);
        response.close();

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.addResource(resource.getName());
        permission.addPolicy(policy.getName());

        response = authorization.permissions().resource().create(permission);
        response.close();
    }

    @Test
    public void testAccessTokenWithUmaAuthorization() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource A");

        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationResponse response = authzClient.authorization("marta", "password").authorize(new AuthorizationRequest(ticket));

        assertNotNull(response.getToken());
    }

    @Test
    public void testResourceServerAsAudience() throws Exception {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest();

        request.setResourceId("Resource A");

        String accessToken = new OAuthClient().realm("authz-test").clientId("test-client").doGrantAccessTokenRequest("secret", "marta", "password").getAccessToken();
        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationResponse response = authzClient.authorization(accessToken).authorize(new AuthorizationRequest(ticket));

        assertNotNull(response.getToken());
        AccessToken rpt = toAccessToken(response.getToken());
        assertEquals("resource-server-test", rpt.getAudience()[0]);
    }

    private RealmResource getRealm() throws Exception {
        return adminClient.realm("authz-test");
    }

    private ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private AuthzClient getAuthzClient() {
        try {
            return AuthzClient.create(JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"), Configuration.class));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to create authz client", cause);
        }
    }
}
