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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractResourceServerTest extends AbstractAuthzTest {

    protected static final String REALM_NAME = "authz-test";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name(REALM_NAME)
                .roles(RolesBuilder.create()
                        .realmRole(RoleBuilder.create().name("uma_authorization").build())
                        .realmRole(RoleBuilder.create().name("uma_protection").build())
                )
                .user(UserBuilder.create().username("marta").password("password")
                        .addRoles("uma_authorization", "uma_protection")
                        .role("resource-server-test", "uma_protection"))
                .user(UserBuilder.create().username("alice").password("password")
                        .addRoles("uma_authorization", "uma_protection")
                        .role("resource-server-test", "uma_protection"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants()
                        .serviceAccountsEnabled(true))
                .client(ClientBuilder.create().clientId("test-app")
                        .redirectUris("http://localhost:8180/auth/realms/master/app/auth", "https://localhost:8543/auth/realms/master/app/auth")
                        .publicClient())
                .testEventListener()
                .build());
    }

    protected AuthorizationResponse authorize(String resourceName, String[] scopeNames, String claimToken) {
        return authorize(null, null, resourceName, scopeNames, null, null, claimToken);
    }

    protected AuthorizationResponse authorize(String resourceName, String[] scopeNames, String claimToken, String tokenFormat) {
        return authorize(null, null, null, null, null, claimToken, tokenFormat, new PermissionRequest(resourceName, scopeNames));
    }

    protected AuthorizationResponse authorize(String resourceName, String[] scopeNames) {
        return authorize(null, null, resourceName, scopeNames, null, null, null);
    }

    protected AuthorizationResponse authorize(String userName, String password, String resourceName, String[] scopeNames) {
        return authorize(userName, password, resourceName, scopeNames, null, null, null);
    }

    protected AuthorizationResponse authorize(String userName, String password, PermissionRequest... permissions) {
        return authorize(userName, password, null, null, null, null, null, permissions);
    }

    protected AuthorizationResponse authorize(String userName, String password, String resourceName, String[] scopeNames, String rpt) {
        return authorize(userName, password, resourceName, scopeNames, null, rpt, null);
    }

    protected AuthorizationResponse authorize(String userName, String password, String resourceName, String[] scopeNames, String[] additionalScopes) {
        return authorize(userName, password, resourceName, scopeNames, additionalScopes, null, null);
    }

    protected AuthorizationResponse authorize(String userName, String password, String resourceName, String[] scopeNames, String[] additionalScopes, String rpt, String claimToken) {
        return authorize(userName, password, additionalScopes, rpt, null, claimToken, null, new PermissionRequest(resourceName, scopeNames));
    }

    protected AuthorizationResponse authorize(String userName, String password, String[] additionalScopes, String rpt, String accessToken, String claimToken, String tokenFormat, PermissionRequest... permissions) {
        ProtectionResource protection;

        if (userName != null) {
            protection = getAuthzClient().protection(userName, password);
        } else {
            protection = getAuthzClient().protection();
        }

        String ticket = protection.permission().create(Arrays.asList(permissions)).getTicket();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(ticket);

        if (additionalScopes != null) {
            StringBuilder builder = new StringBuilder();

            for (String scope : additionalScopes) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(scope);
            }

            authorizationRequest.setScope(builder.toString());
        }

        authorizationRequest.setRpt(rpt);
        authorizationRequest.setClaimTokenFormat(tokenFormat);
        authorizationRequest.setClaimToken(claimToken);

        org.keycloak.authorization.client.resource.AuthorizationResource authorization;

        if (userName != null) {
            authorization = getAuthzClient().authorization(userName, password);
        } else if (accessToken != null) {
            authorization = getAuthzClient().authorization(accessToken);
        } else {
            authorization = getAuthzClient().authorization();
        }

        return authorization.authorize(authorizationRequest);
    }

    protected RealmResource getRealm() {
        return adminClient.realm("authz-test");
    }

    protected ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    protected AuthzClient getAuthzClient() {
        try {
            return AuthzClient.create(httpsAwareConfigurationStream(getClass().getResourceAsStream("/authorization-test/default-keycloak-uma2.json")));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to create authz client", cause);
        }
    }

    protected void assertPermissions(Collection<Permission> permissions, String expectedResource, String... expectedScopes) {
        Iterator<Permission> iterator = permissions.iterator();

        while (iterator.hasNext()) {
            Permission permission = iterator.next();

            if (permission.getResourceName().equalsIgnoreCase(expectedResource) || permission.getResourceId().equals(expectedResource)) {
                Set<String> scopes = permission.getScopes();

                assertEquals(expectedScopes.length, scopes.size());

                if (scopes.containsAll(Arrays.asList(expectedScopes))) {
                    iterator.remove();
                }
            }
        }
    }

    protected ResourceRepresentation addResource(String resourceName, String... scopeNames) throws Exception {
        return addResource(resourceName, null, false, scopeNames);
    }

    protected ResourceRepresentation addResource(String resourceName, boolean ownerManagedAccess, String... scopeNames) throws Exception {
        return addResource(resourceName, null, ownerManagedAccess, scopeNames);
    }

    protected ResourceRepresentation addResource(String resourceName, String owner, boolean ownerManagedAccess, String... scopeNames) throws Exception {
        ClientResource client = getClient(getRealm());
        AuthorizationResource authorization = client.authorization();
        ResourceRepresentation resource = new ResourceRepresentation(resourceName);

        if (owner != null) {
            resource.setOwner(new ResourceOwnerRepresentation(owner));
        }

        resource.setOwnerManagedAccess(ownerManagedAccess);
        resource.addScope(scopeNames);

        Response response = authorization.resources().create(resource);
        ResourceRepresentation temp = response.readEntity(ResourceRepresentation.class);
        resource.setId(temp.getId());
        response.close();

        return resource;
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }
}
