package org.keycloak.testsuite.ui.account2;

import com.google.common.collect.Lists;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AccountRoles;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PermissionTicketRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.MyResourcesPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.NoSuchElementException;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MyResourcesTest extends BaseAccountPageTest {
    private static final String[] userNames = new String[]{"alice", "jdoe"};

    @Page
    private MyResourcesPage myResourcesPage;

    private RealmRepresentation testRealm;
    private CloseableHttpClient httpClient;

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return myResourcesPage;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        testRealm = testRealms.get(0);
        testRealm.setUserManagedAccessAllowed(true);

        testRealm.setUsers(Lists.asList("admin", userNames).stream().map(this::createUser).collect(Collectors.toList()));

        ClientRepresentation client = ClientBuilder.create()
                .clientId("my-resource-server")
                .authorizationServicesEnabled(true)
                .serviceAccountsEnabled(true)
                .secret("secret")
                .name("My Resource Server")
                .baseUrl("http://resourceserver.com")
                .directAccessGrants().build();

        testRealm.setClients(singletonList(client));
    }

    private UserRepresentation createUser(String userName) {
        return UserBuilder.create()
                .username(userName)
                .enabled(true)
                .password(PASSWORD)
                .role("account", AccountRoles.MANAGE_ACCOUNT)
                .build();
    }

    @After
    public void after() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void afterAbstractKeycloakTestRealmImport() {
        ClientResource resourceServer = getResourceServer();
        AuthzClient authzClient = createAuthzClient(resourceServer.toRepresentation());
        AuthorizationResource authorization = resourceServer.authorization();
        ResourceRepresentation resource13 = null;
        for (int i = 1; i < 15; i++) {
            ResourceRepresentation resource = createResource(authzClient, authorization, i);
            if (i == 13) {
                resource13 = resource;
            }

            for (String scope : Arrays.asList("Scope A", "Scope B")) {
                createTicket(authzClient, i, resource, scope, userNames[i % userNames.length]);
            }
        }

        createTicket(authzClient, 13, resource13, "Scope A", "admin");
    }

    private void createTicket(AuthzClient authzClient, int i, ResourceRepresentation resource, String scope, String userName) {
        PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();

        ticket.setGranted(!(i == 12 || i == 13));
        ticket.setOwner(resource.getOwner().getId());
        ticket.setRequesterName(userName);
        ticket.setResource(resource.getId());
        ticket.setScopeName(scope);

        authzClient.protection("jdoe", PASSWORD).permission().create(ticket);
    }

    private ResourceRepresentation createResource(AuthzClient authzClient, AuthorizationResource authorization, int i) {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setOwnerManagedAccess(true);

        try {
            final byte[] content = new JWSInput(authzClient.obtainAccessToken("jdoe", PASSWORD).getToken()).getContent();
            final AccessToken accessToken = JsonSerialization.readValue(content, AccessToken.class);
            resource.setOwner(accessToken.getSubject());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        resource.setName("Resource " + i);
        resource.setDisplayName("Display Name " + i);
        resource.setIconUri("Icon Uri " + i);
        resource.addScope("Scope A", "Scope B", "Scope C", "Scope D");
        resource.setUri("http://resourceServer.com/resources/" + i);

        try (Response response1 = authorization.resources().create(resource)) {
            resource.setId(response1.readEntity(ResourceRepresentation.class).getId());
        }
        return resource;
    }

    @Override
    public void addTestUser() {
        testUser = createUser("jdoe");
    }

    @Test
    public void shouldDisplayTheResources() {
        assertEquals(6, myResourcesPage.getResourcesListCount());

        assertEquals("Resource 0", myResourcesPage.getCellText("name", 0));
        assertEquals("Resource 1", myResourcesPage.getCellText("name", 1));
        assertEquals("My Resource Server", myResourcesPage.getCellText("client", 0));
        assertEquals("http://resourceserver.com/", myResourcesPage.getCellHref("client", 0));
    }

    @Test
    public void shouldShowMyResourcesAndUpdatePermissions() {
        final int row = 1;
        myResourcesPage.clickExpandButton(row);
        myResourcesPage.clickCollapseButton(row);
        myResourcesPage.clickExpandButton(row);

        assertEquals("Resource is shared with jdoe.", myResourcesPage.getSharedWith(row));

        myResourcesPage.clickEditButton(row);
        assertEquals("jdoe", myResourcesPage.getEditDialogUsername(0));
        myResourcesPage.removeAllPermissions();

        myResourcesPage.alert().assertSuccess();
        assertEquals("This resource is not shared.", myResourcesPage.getSharedWith(row));
    }

    @Test
    public void shouldShowMyResourcesAndRemoveShares() {
        final int row = 2;
        myResourcesPage.clickExpandButton(row);

        testModalDialog(() -> myResourcesPage.clickRemoveButton(row), () ->
                assertEquals("Resource is shared with alice.", myResourcesPage.getSharedWith(row)));

        myResourcesPage.alert().assertSuccess();
        assertEquals("This resource is not shared.", myResourcesPage.getSharedWith(row));
    }

    @Test
    public void shouldShowMyResourcesAndShare() {
        final int row = 3;
        myResourcesPage.clickExpandButton(row);

        assertEquals("Resource is shared with jdoe.", myResourcesPage.getSharedWith(row));

        myResourcesPage.clickShareButton(row);
        myResourcesPage.createShare("alice");

        myResourcesPage.alert().assertSuccess();
        assertThat(myResourcesPage.getSharedWith(row), endsWith("and 1 other users."));
    }

    @Test
    public void firstShouldRefreshOnRefreshButtonClick() {
        ClientResource resourceServer = getResourceServer();
        AuthzClient authzClient = createAuthzClient(resourceServer.toRepresentation());
        AuthorizationResource authorization = resourceServer.authorization();

        createResource(authzClient, authorization, 0);

        assertEquals("Resource 1", myResourcesPage.getCellText("name", 0));
        myResourcesPage.clickRefreshButton();
        assertEquals("Resource 0", myResourcesPage.getCellText("name", 0));
    }

    @Test
    public void shouldAllowRequestToShare() {
        final String resourceName = "Resource 12";
        assertEquals("1", myResourcesPage.getPendingRequestText(resourceName));

        switchUserSharedWithMeTab();
        assertFalse(myResourcesPage.containsResource(resourceName));
        login("jdoe");

        myResourcesPage.clickPendingRequest(resourceName);
        assertEquals("alice", myResourcesPage.getPendingRequestRequestor(0));
        final String permissions = myResourcesPage.getPendingRequestPermissions(0);
        assertTrue(permissions.contains("Scope A"));
        assertTrue(permissions.contains("Scope B"));
        myResourcesPage.acceptRequest(resourceName, 0);

        assertNoPendingRequest(resourceName);
        switchUserSharedWithMeTab();

        assertTrue(myResourcesPage.containsResource(resourceName));
    }

    @Test
    public void shouldDenyRequestToShare() {
        final String resourceName = "Resource 13";

        switchUserSharedWithMeTab();
        assertFalse(myResourcesPage.containsResource(resourceName));
        login("jdoe");

        myResourcesPage.clickNextPage();
        assertEquals("2", myResourcesPage.getPendingRequestText(resourceName));
        myResourcesPage.clickPendingRequest(resourceName);
        myResourcesPage.denyRequest(resourceName, 0);

        assertEquals("1", myResourcesPage.getPendingRequestText(resourceName));

        switchUserSharedWithMeTab();
        assertFalse(myResourcesPage.containsResource(resourceName));
    }

    @Test
    public void shouldNotHaveRequestToShareButton() {
        assertNoPendingRequest("Resource 0");
        assertNoPendingRequest("Resource 1");
    }

    private void assertNoPendingRequest(String resourceName) {
        try {
            myResourcesPage.getPendingRequestText(resourceName);
            fail("should not have a pending request button");
        } catch (NoSuchElementException e) {
            // success
        }
    }

    private void switchUserSharedWithMeTab() {
        login("alice");
        myResourcesPage.clickSharedWithMeTab();
    }

    private void login(String user) {
        myResourcesPage.clickSignOut();
        myResourcesPage.navigateTo();
        loginPage.form().login(createUser(user));
    }

    private AuthzClient createAuthzClient(ClientRepresentation client) {
        Map<String, Object> credentials = new HashMap<>();

        credentials.put("secret", "secret");
        httpClient = HttpClientBuilder.create().build();

        return AuthzClient
                .create(new Configuration(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth",
                        testRealm.getRealm(), client.getClientId(),
                        credentials, httpClient));
    }

    private ClientResource getResourceServer() {
        ClientsResource clients = adminClient.realm(TEST).clients();
        return clients.get(clients.findByClientId("my-resource-server").get(0).getId());
    }
}
