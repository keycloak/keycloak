/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.account;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.services.resources.account.resources.AbstractResourceService;
import org.keycloak.services.resources.account.resources.AbstractResourceService.Permission;
import org.keycloak.services.resources.account.resources.AbstractResourceService.Resource;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.keycloak.common.util.Encode.encodePathAsIs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcesRestServiceTest extends AbstractRestServiceTest {

    private AuthzClient authzClient;
    private List<String> userNames = new ArrayList<>(Arrays.asList("alice", "jdoe", "bob"));

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(Profile.Feature.AUTHORIZATION);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        RealmRepresentation realmRepresentation = testRealm;

        realmRepresentation.setUserManagedAccessAllowed(true);

        testRealm.getUsers().add(createUser("alice", "password"));
        testRealm.getUsers().add(createUser("jdoe", "password"));
        testRealm.getUsers().add(createUser("bob", "password"));

        ClientRepresentation client = ClientBuilder.create()
                .clientId("my-resource-server")
                .authorizationServicesEnabled(true)
                .serviceAccountsEnabled(true)
                .secret("secret")
                .name("My Resource Server")
                .baseUrl("http://resourceserver.com")
                .directAccessGrants().build();

        testRealm.getClients().add(client);
    }

    @Override
    public void before() {
        super.before();
        ClientResource resourceServer = getResourceServer();
        authzClient = createAuthzClient(resourceServer.toRepresentation());
        AuthorizationResource authorization = resourceServer.authorization();

        for (int i = 0; i < 30; i++) {
            ResourceRepresentation resource = new ResourceRepresentation();

            resource.setOwnerManagedAccess(true);

            try {
                resource.setOwner(
                        JsonSerialization.readValue(new JWSInput(tokenUtil.getToken()).getContent(), AccessToken.class)
                                .getSubject());
            } catch (Exception cause) {
                throw new RuntimeException("Failed to parse access token", cause);
            }

            resource.setName("Resource " + i);
            resource.setDisplayName("Display Name " + i);
            resource.setIconUri("Icon Uri " + i);
            resource.addScope("Scope A", "Scope B", "Scope C", "Scope D");
            resource.setUri("http://resourceServer.com/resources/" + i);

            try (Response response1 = authorization.resources().create(resource)) {
                resource.setId(response1.readEntity(ResourceRepresentation.class).getId());
                assertTrue(resource.getId() != null);
            }

            for (String scope : Arrays.asList("Scope A", "Scope B")) {
                PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();

                ticket.setGranted(true);
                ticket.setOwner(resource.getOwner().getId());
                ticket.setRequesterName(userNames.get(i % userNames.size()));
                ticket.setResource(resource.getId());
                ticket.setScopeName(scope);

                authzClient.protection("test-user@localhost", "password").permission().create(ticket);
            }
        }
    }

    private ClientResource getResourceServer() {
        ClientsResource clients = testRealm().clients();
        return clients.get(clients.findByClientId("my-resource-server").get(0).getId());
    }

    @Override
    public void after() {
        super.after();
        ClientResource resourceServer = getResourceServer();
        ClientRepresentation representation = resourceServer.toRepresentation();
        representation.setAuthorizationServicesEnabled(false);
        resourceServer.update(representation);
        representation.setAuthorizationServicesEnabled(true);
        resourceServer.update(representation);
    }

    @Test
    public void testGetMyResources() {
        List<Resource> resources = getMyResources();

        assertEquals(30, resources.size());
        assertMyResourcesResponse(resources);
    }

    @Test
    public void testGetMyResourcesByName() {
        assertEquals(11, getMyResources("Resource 1").size());
        assertEquals(0, getMyResources("non-existent\n").size());
        assertEquals(1, getMyResources("Resource 23").size());
    }

    @Test
    public void testGetMyResourcesPagination() {
        List<Resource> resources = getMyResources(0, 10, response -> assertNextPageLink(response, "/realms/test/account/resources", 10, -1, 10));

        assertEquals(10, resources.size());
        assertMyResourcesResponse(resources);

        resources = getMyResources(10, 10, response -> assertNextPageLink(response, "/realms/test/account/resources", 20, 0, 10));

        assertEquals(10, resources.size());

        resources = getMyResources(20, 10, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", -1, 10, 10);
        });

        getMyResources(15, 5, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", 20, 10, 5);
        });

        assertEquals(10, resources.size());

        resources = getMyResources(30, 10);

        assertEquals(0, resources.size());

        getMyResources(5, 10, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", 15, 0, 10);
        });

        getMyResources(10, 10, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", 20, 0, 10);
        });

        getMyResources(20, 10, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", -1, 10, 10);
        });

        getMyResources(20, 20, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", -1, 0, 20);
        });

        getMyResources(30, 30, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", -1, -1, 30);
        });

        getMyResources(30, 31, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", -1, -1, 31);
        });

        getMyResources(0, 30, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", -1, -1, 30);
        });

        getMyResources(0, 31, response -> {
            assertNextPageLink(response, "/realms/test/account/resources", -1, -1, 31);
        });
    }

    @Test
    public void testGetSharedWithMe() {
        for (String userName : userNames) {
            List<AbstractResourceService.ResourcePermission> resources = getSharedWithMe(userName);

            assertEquals(10, resources.size());
            assertSharedWithMeResponse(resources);
        }
    }

    @Test
    public void testGetSharedWithMeByName() {
        assertEquals(5, getSharedWithMe("jdoe", "Resource 1", -1, -1, null).size());
        assertEquals(0, getSharedWithMe("jdoe", "non-existent", -1, -1, null).size());
        assertEquals(10, getSharedWithMe("jdoe", "resource", -1, -1, null).size());
    }

    @Test
    public void testGetSharedWithMePagination() {
        for (String userName : userNames) {
            List<AbstractResourceService.ResourcePermission> resources = getSharedWithMe(userName, null, 0, 3,
                    response -> assertNextPageLink(response, "/realms/test/account/resources/shared-with-me", 3, -1, 3));

            assertSharedWithMeResponse(resources);

            getSharedWithMe(userName, null, 3, 3,
                    response -> assertNextPageLink(response, "/realms/test/account/resources/shared-with-me", 6, 0, 3));
            getSharedWithMe(userName, null, 6, 3,
                    response -> assertNextPageLink(response, "/realms/test/account/resources/shared-with-me", 9, 3, 3));
            getSharedWithMe(userName, null, 9, 3,
                    response -> assertNextPageLink(response, "/realms/test/account/resources/shared-with-me", -1, 6, 3));
        }
    }

    @Test
    public void testGetSharedWithOthers() {
        List<AbstractResourceService.ResourcePermission> resources = doGet("/shared-with-others",
                new TypeReference<List<AbstractResourceService.ResourcePermission>>() {
                });

        assertEquals(30, resources.size());
        assertSharedWithOthersResponse(resources);
    }

    @Test
    public void testGetSharedWithOthersPagination() {
        List<AbstractResourceService.ResourcePermission> resources = doGet("/shared-with-others?first=0&max=5",
                new TypeReference<List<AbstractResourceService.ResourcePermission>>() {
                }, response -> assertNextPageLink(response, "/realms/test/account/resources/shared-with-others", 5, -1, 5));

        assertEquals(5, resources.size());
        assertSharedWithOthersResponse(resources);

        doGet("/shared-with-others?first=5&max=5",
                new TypeReference<List<AbstractResourceService.ResourcePermission>>() {
                }, response -> assertNextPageLink(response, "/realms/test/account/resources/shared-with-others", 10, 0, 5));
        doGet("/shared-with-others?first=20&max=5",
                new TypeReference<List<AbstractResourceService.ResourcePermission>>() {
                }, response -> assertNextPageLink(response, "/realms/test/account/resources/shared-with-others", 25, 15, 5));
        doGet("/shared-with-others?first=25&max=5",
                new TypeReference<List<AbstractResourceService.ResourcePermission>>() {
                }, response -> assertNextPageLink(response, "/realms/test/account/resources/shared-with-others", -1, 20, 5));
    }

    @Test
    public void testGetResource() {
        Resource resource = doGet("/" + encodePathAsIs(getMyResources().get(0).getId()), Resource.class);

        String uri = resource.getUri();
        int id = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
        assertNotNull(resource.getId());
        assertEquals("Resource " + id, resource.getName());
        assertEquals("Display Name " + id, resource.getDisplayName());
        assertEquals("Icon Uri " + id, resource.getIconUri());
        assertEquals("my-resource-server", resource.getClient().getClientId());
        assertEquals("My Resource Server", resource.getClient().getName());
        assertEquals("http://resourceserver.com", resource.getClient().getBaseUrl());
        assertEquals(4, resource.getScopes().size());

        OAuth2ErrorRepresentation response = doGet("/invalid_resource", OAuth2ErrorRepresentation.class);
        assertEquals("resource_not_found", response.getError());

        response = doGet("/" + encodePathAsIs(getMyResources().get(0).getId()), authzClient.obtainAccessToken("jdoe", "password").getToken(), OAuth2ErrorRepresentation.class);
        assertEquals("invalid_resource", response.getError());
    }

    @Test
    public void testGetPermissions() throws Exception {
        Resource resource = getMyResources().get(0);
        List<Permission> shares = doGet("/" + encodePathAsIs(resource.getId()) + "/permissions", new TypeReference<List<Permission>>() {});

        assertEquals(1, shares.size());

        Permission firstShare = shares.get(0);
        List<Permission> permissions = new ArrayList<>();

        assertTrue(userNames.contains(firstShare.getUsername()));
        assertEquals(2, firstShare.getScopes().size());

        List<String> users = new ArrayList<>(userNames);

        users.remove(firstShare.getUsername());

        for (String userName : users) {
            Permission permission = new Permission();

            permission.setUsername(userName);
            permission.addScope("Scope D");

            permissions.add(permission);
        }

        SimpleHttpDefault.doPut(getAccountUrl("resources/" + encodePathAsIs(resource.getId()) + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(permissions).asResponse();

        shares = doGet("/" + encodePathAsIs(resource.getId()) + "/permissions", new TypeReference<List<Permission>>() {});

        assertEquals(3, shares.size());

        for (Permission user : shares) {
            assertTrue(userNames.contains(user.getUsername()));

            if (firstShare.getUsername().equals(user.getUsername())) {
                assertEquals(2, user.getScopes().size());
            } else {
                assertEquals(1, user.getScopes().size());
            }
        }
    }

    @Test
    public void testShareResource() throws Exception {
        List<String> users = new LinkedList<>(Arrays.asList("jdoe", "alice"));
        List<Permission> permissions = new ArrayList<>();
        AbstractResourceService.ResourcePermission sharedResource = null;

        for (String user : users) {
            sharedResource = getSharedWithMe(user).get(0);

            assertNotNull(sharedResource);
            assertEquals(2, sharedResource.getScopes().size());
        }

        permissions.add(new Permission(users.get(0), "Scope C", "Scope D"));
        permissions.add(new Permission(users.get(users.size() - 1), "Scope A", "Scope B", "Scope C", "Scope D"));

        String resourceId = sharedResource.getId();
        SimpleHttpResponse response = SimpleHttpDefault.doPut(getAccountUrl("resources/" + encodePathAsIs(resourceId) + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(permissions).asResponse();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        for (String user : users) {
            sharedResource = getSharedWithMe(user).stream()
                    .filter(resource1 -> resource1.getId().equals(resourceId)).findAny().orElse(null);

            assertNotNull(sharedResource);

            if (user.equals(users.get(users.size() - 1))) {
                assertEquals(4, sharedResource.getScopes().size());
            } else {
                assertEquals(2, sharedResource.getScopes().size());
            }
        }
    }

    @Test
    public void failShareResourceInvalidPermissions() throws Exception {
        List<Permission> permissions = new ArrayList<>();

        SimpleHttpResponse response = SimpleHttpDefault.doPut(getAccountUrl("resources/" + encodePathAsIs(getMyResources().get(0).getId()) + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(permissions).asResponse();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testEndpointPermissions() throws Exception {
        // resource for view-account-access
        String resourceId;
        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setOwnerManagedAccess(true);
        resource.setOwner(findUser("view-account-access").getId());
        resource.setName("Resource view-account-access");
        resource.setDisplayName("Display Name view-account-access");
        resource.setIconUri("Icon Uri view-account-access");
        resource.addScope("Scope A", "Scope B", "Scope C", "Scope D");
        resource.setUri("http://resourceServer.com/resources/view-account-access");
        try (Response response1 = getResourceServer().authorization().resources().create(resource)) {
            resourceId = response1.readEntity(ResourceRepresentation.class).getId();
        }

        final String resourcesUrl = getAccountUrl("resources");
        final String sharedWithOthersUrl = resourcesUrl + "/shared-with-others";
        final String sharedWithMeUrl = resourcesUrl + "/shared-with-me";
        final String resourceUrl = resourcesUrl + "/" + encodePathAsIs(resourceId);
        final String permissionsUrl = resourceUrl + "/permissions";
        final String requestsUrl = resourceUrl + "/permissions/requests";

        TokenUtil viewProfileTokenUtil = new TokenUtil("view-account-access", "password");
        TokenUtil noAccessTokenUtil = new TokenUtil("no-account-access", "password");

        // test read access
        for (String url : Arrays.asList(resourcesUrl, sharedWithOthersUrl, sharedWithMeUrl, resourceUrl, permissionsUrl, requestsUrl)) {
            assertEquals( "no-account-access GET " + url, 403,
                    SimpleHttpDefault.doGet(url, httpClient).acceptJson().auth(noAccessTokenUtil.getToken()).asStatus());
            assertEquals("view-account-access GET " + url,200,
                    SimpleHttpDefault.doGet(url, httpClient).acceptJson().auth(viewProfileTokenUtil.getToken()).asStatus());
        }

        // test write access
        assertEquals( "no-account-access PUT " + permissionsUrl, 403,
                SimpleHttpDefault.doPut(permissionsUrl, httpClient).acceptJson().auth(noAccessTokenUtil.getToken()).json(Collections.emptyList()).asStatus());
        assertEquals( "view-account-access PUT " + permissionsUrl, 403,
                SimpleHttpDefault.doPut(permissionsUrl, httpClient).acceptJson().auth(viewProfileTokenUtil.getToken()).json(Collections.emptyList()).asStatus());
    }

    @Test
    public void testRevokePermission() throws Exception {
        List<String> users = Arrays.asList("jdoe", "alice");
        List<Permission> permissions = new ArrayList<>();
        AbstractResourceService.ResourcePermission sharedResource = null;

        for (String user : users) {
            sharedResource = getSharedWithMe(user).get(0);

            assertNotNull(sharedResource);
            assertEquals(2, sharedResource.getScopes().size());
        }

        permissions.add(new Permission(users.get(0), "Scope C"));
        permissions.add(new Permission(users.get(users.size() - 1), "Scope B", "Scope D"));

        String resourceId = sharedResource.getId();
        SimpleHttpResponse response = SimpleHttpDefault.doPut(getAccountUrl("resources/" + encodePathAsIs(resourceId) + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(permissions).asResponse();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        for (String user : users) {
            sharedResource = getSharedWithMe(user).stream()
                    .filter(resource1 -> resource1.getId().equals(resourceId)).findAny().orElse(null);

            assertNotNull(sharedResource);

            if (user.equals(users.get(users.size() - 1))) {
                assertEquals(2, sharedResource.getScopes().size());
            } else {
                assertEquals(1, sharedResource.getScopes().size());
            }
        }
    }

    @Test
    public void testGetPermissionRequests() {
        Resource resource = getMyResources().get(0);
        List<Permission> requests = doGet("/" + encodePathAsIs(resource.getId()) + "/permissions/requests",
                new TypeReference<List<Permission>>() {});

        assertTrue(requests.isEmpty());

        for (String userName : userNames) {
            List<String> scopes = new ArrayList<>();

            if ("bob".equals(userName)) {
                scopes.add("Scope D");
            } else if ("alice".equals(userName)) {
                scopes.add("Scope C");
            } else if ("jdoe".equals(userName)) {
                scopes.add("Scope C");
                scopes.add("Scope D");
            }

            for (String scope : scopes) {
                PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();

                ticket.setGranted(false);
                ticket.setOwner("test-user@localhost");
                ticket.setRequesterName(userName);
                ticket.setResource(resource.getId());
                ticket.setScopeName(scope);

                authzClient.protection("test-user@localhost", "password").permission().create(ticket);
            }
        }

        requests = doGet("/" + encodePathAsIs(resource.getId()) + "/permissions/requests",
                new TypeReference<List<Permission>>() {});

        assertEquals(3, requests.size());

        Iterator<Permission> iterator = requests.iterator();

        while (iterator.hasNext()) {
            Permission permission = iterator.next();
            String username = permission.getUsername();
            List<String> scopes = permission.getScopes();

            if ("bob".equals(username)) {
                assertEquals(1, scopes.size());
                assertTrue(scopes.contains("Scope D"));
                iterator.remove();
            } else if ("alice".equals(username)) {
                assertEquals(1, scopes.size());
                assertTrue(scopes.contains("Scope C"));
                iterator.remove();
            } else if ("jdoe".equals(username)) {
                assertEquals(2, scopes.size());
                assertTrue(scopes.contains("Scope C"));
                assertTrue(scopes.contains("Scope D"));
                iterator.remove();
            }
        }

        assertTrue(requests.isEmpty());
    }

    @Test
    public void testApprovePermissionRequest() throws IOException {
        Resource resource = getMyResources().get(0);
        List<Permission> requests = doGet("/" + encodePathAsIs(resource.getId()) + "/permissions/requests",
                new TypeReference<List<Permission>>() {});

        assertTrue(requests.isEmpty());

        for (String userName : userNames) {
            List<String> scopes = new ArrayList<>();

            if ("bob".equals(userName)) {
                scopes.add("Scope D");
            } else if ("alice".equals(userName)) {
                scopes.add("Scope C");
            } else if ("jdoe".equals(userName)) {
                scopes.add("Scope C");
                scopes.add("Scope D");
            }

            for (String scope : scopes) {
                PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();

                ticket.setGranted(false);
                ticket.setOwner("test-user@localhost");
                ticket.setRequesterName(userName);
                ticket.setResource(resource.getId());
                ticket.setScopeName(scope);

                authzClient.protection("test-user@localhost", "password").permission().create(ticket);
            }
        }

        requests = doGet("/" + encodePathAsIs(resource.getId()) + "/permissions/requests",
                new TypeReference<List<Permission>>() {});

        assertEquals(3, requests.size());

        Iterator<Permission> iterator = requests.iterator();

        while (iterator.hasNext()) {
            Permission permission = iterator.next();
            String username = permission.getUsername();
            List<String> scopes = permission.getScopes();

            if ("bob".equals(username)) {
                scopes.clear();
            } else if ("jdoe".equals(username)) {
                scopes.remove("Scope C");
            }
        }

        SimpleHttpDefault.doPut(getAccountUrl("resources/" + encodePathAsIs(resource.getId()) + "/permissions"), httpClient)
                .auth(tokenUtil.getToken())
                .json(requests).asResponse();

        requests = doGet("/" + encodePathAsIs(resource.getId()) + "/permissions/requests",
                new TypeReference<List<Permission>>() {});

        assertThat(requests, empty());

        for (String user : Arrays.asList("alice", "jdoe")) {
            AbstractResourceService.ResourcePermission sharedResource = getSharedWithMe(user).stream()
                    .filter(resource1 -> resource1.getId().equals(resource.getId())).findAny().orElse(null);

            assertNotNull(sharedResource);

            Set<ScopeRepresentation> scopes = sharedResource.getScopes();

            if ("alice".equals(user)) {
                assertEquals(1, scopes.size());
                assertTrue(scopes.stream().anyMatch(scope -> "Scope C".equals(scope.getName())));
            } else if ("jdoe".equals(user)) {
                assertEquals(1, scopes.size());
                assertTrue(scopes.stream().anyMatch(scope -> "Scope D".equals(scope.getName())));
            }
        }
    }

    private List<AbstractResourceService.ResourcePermission> getSharedWithMe(String userName) {
        return getSharedWithMe(userName, null, -1, -1, null);
    }

    private List<AbstractResourceService.ResourcePermission> getSharedWithMe(String userName, String name, int first, int max, Consumer<SimpleHttpResponse> responseHandler) {
        KeycloakUriBuilder uri = KeycloakUriBuilder.fromUri("/shared-with-me");

        if (name != null) {
            uri.queryParam("name", name);
        }

        if (first > -1 && max > -1) {
            uri.queryParam("first", first);
            uri.queryParam("max", max);
        }

        return doGet(uri.build().toString(), authzClient.obtainAccessToken(userName, "password").getToken(),
                new TypeReference<List<AbstractResourceService.ResourcePermission>>() {}, responseHandler);
    }

    private <R> R doGet(String resource, TypeReference<R> typeReference) {
        return doGet(resource, tokenUtil.getToken(), typeReference);
    }

    private <R> R doGet(String resource, TypeReference<R> typeReference, Consumer<SimpleHttpResponse> response) {
        return doGet(resource, tokenUtil.getToken(), typeReference, response);
    }

    private <R> R doGet(String resource, Class<R> type) {
        return doGet(resource, tokenUtil.getToken(), type);
    }

    private <R> R doGet(String resource, String token, TypeReference<R> typeReference) {
        try {
            return get(resource, token).asJson(typeReference);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to fetch resource", cause);
        }
    }

    private <R> R doGet(String resource, String token, TypeReference<R> typeReference, Consumer<SimpleHttpResponse> responseHandler) {
        try {
            SimpleHttpRequest http = get(resource, token);

            http.header("Accept", "application/json");
            SimpleHttpResponse response = http.asResponse();

            if (responseHandler != null) {
                responseHandler.accept(response);
            }

            R result = JsonSerialization.readValue(response.asString(), typeReference);

            return result;
        } catch (IOException cause) {
            throw new RuntimeException("Failed to fetch resource", cause);
        }
    }

    private <R> R doGet(String resource, String token, Class<R> type) {
        try {
            return get(resource, token).asJson(type);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to fetch resource", cause);
        }
    }

    private SimpleHttpRequest get(String resource, String token) {
        return SimpleHttpDefault.doGet(getAccountUrl("resources" + resource), httpClient).auth(token);
    }

    private AuthzClient createAuthzClient(ClientRepresentation client) {
        Map<String, Object> credentials = new HashMap<>();

        credentials.put("secret", "secret");

        return AuthzClient
                .create(new Configuration(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth",
                        testRealm().toRepresentation().getRealm(), client.getClientId(),
                        credentials, httpClient));
    }

    private UserRepresentation createUser(String userName, String password) {
        return UserBuilder.create()
                .username(userName)
                .enabled(true)
                .password(password)
                .role("account", AccountRoles.MANAGE_ACCOUNT)
                .build();
    }

    private List<Resource> getMyResources() {
        return getMyResources(-1, -1);
    }

    private List<Resource> getMyResources(String name) {
        return getMyResources(name, -1, -1);
    }

    private List<Resource> getMyResources(int first, int max) {
        return getMyResources(null, first, max);
    }

    private List<Resource> getMyResources(String name, int first, int max) {
        KeycloakUriBuilder uri = KeycloakUriBuilder.fromUri("");

        if (name != null) {
            uri.queryParam("name", name);
        }

        if (first > -1 && max > -1) {
            uri.queryParam("first", first);
            uri.queryParam("max", max);
        }

        return doGet(uri.build().toString(), new TypeReference<List<Resource>>() {});
    }

    private List<Resource> getMyResources(int first, int max, Consumer<SimpleHttpResponse> response) {
        String query = "";
        if (first > -1 && max > -1) {
            query = "?first=" + first + "&max=" + max;
        }
        return doGet(query, new TypeReference<List<Resource>>() {}, response);
    }

    private void assertSharedWithOthersResponse(List<AbstractResourceService.ResourcePermission> resources) {
        for (AbstractResourceService.ResourcePermission resource : resources) {
            String uri = resource.getUri();
            int id = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
            assertNotNull(resource.getId());
            assertEquals("Resource " + id, resource.getName());
            assertEquals("Display Name " + id, resource.getDisplayName());
            assertEquals("Icon Uri " + id, resource.getIconUri());
            assertEquals("my-resource-server", resource.getClient().getClientId());
            assertEquals("My Resource Server", resource.getClient().getName());
            assertEquals("http://resourceserver.com", resource.getClient().getBaseUrl());
            assertEquals(1, resource.getPermissions().size());
            Permission user = resource.getPermissions().iterator().next();

            assertTrue(userNames.contains(user.getUsername()));

            assertEquals(2, user.getScopes().size());
        }
    }

    private void assertMyResourcesResponse(List<Resource> resources) {
        for (Resource resource : resources) {
            String uri = resource.getUri();
            int id = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));

            assertNotNull(resource.getId());
            assertEquals("Resource " + id, resource.getName());
            assertEquals("Display Name " + id, resource.getDisplayName());
            assertEquals("Icon Uri " + id, resource.getIconUri());
            assertEquals("my-resource-server", resource.getClient().getClientId());
            assertEquals("My Resource Server", resource.getClient().getName());
            assertEquals("http://resourceserver.com", resource.getClient().getBaseUrl());
        }
    }

    private void assertSharedWithMeResponse(List<AbstractResourceService.ResourcePermission> resources) {
        for (AbstractResourceService.ResourcePermission resource : resources) {
            String uri = resource.getUri();
            int id = Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1));
            assertNotNull(resource.getId());
            assertEquals("Resource " + id, resource.getName());
            assertEquals("Display Name " + id, resource.getDisplayName());
            assertEquals("Icon Uri " + id, resource.getIconUri());
            assertEquals("my-resource-server", resource.getClient().getClientId());
            assertEquals("My Resource Server", resource.getClient().getName());
            assertEquals("http://resourceserver.com", resource.getClient().getBaseUrl());
            assertEquals(2, resource.getScopes().size());
        }
    }

    private void assertNextPageLink(SimpleHttpResponse response, String uri, int nextPage, int previousPage, int max) {
        try {
            List<String> links = response.getHeader("Link");

            if (nextPage == -1 && previousPage == -1) {
                assertNull(links);
                return;
            }

            assertNotNull(links);

            assertEquals(nextPage > -1 && previousPage > -1 ? 2 : 1, links.size());

            for (String link : links) {
                if (link.contains("rel=\"next\"")) {
                    assertEquals("<" + authzClient.getConfiguration().getAuthServerUrl() + uri + "?first=" + nextPage + "&max=" + max + ">; rel=\"next\"", link);
                } else {
                    assertEquals("<" + authzClient.getConfiguration().getAuthServerUrl() + uri + "?first=" + previousPage + "&max=" + max + ">; rel=\"prev\"", link);
                }
            }
        } catch (IOException e) {
            fail("Fail to get link header");
        }
    }
}
