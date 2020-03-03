package org.keycloak.testsuite.ui.account2;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

public class MyResourcesTest extends BaseAccountPageTest {
    private static final String[] userNames = new String[]{"alice", "jdoe", "bob"};

    @Page
    private MyResourcesPage myResourcesPage;

    private AuthzClient authzClient;
    private RealmRepresentation testRealm;
    private CloseableHttpClient httpClient;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        testRealm = testRealms.get(0);
        testRealm.setUserManagedAccessAllowed(true);

        testRealm.setUsers(Arrays.asList(createUser("alice"),
                createUser("jdoe"), createUser("bob")));

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
                .password("password")
                .role("account", AccountRoles.MANAGE_ACCOUNT)
                .build();
    }

    @Before
    public void before() {
        httpClient = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setup() throws Exception {
        ClientResource resourceServer = getResourceServer();
        authzClient = createAuthzClient(resourceServer.toRepresentation());
        AuthorizationResource authorization = resourceServer.authorization();

        for (int i = 0; i < 30; i++) {
            ResourceRepresentation resource = new ResourceRepresentation();

            resource.setOwnerManagedAccess(true);

            final byte[] content = new JWSInput(authzClient.obtainAccessToken("jdoe", "password").getToken()).getContent();
            final AccessToken accessToken = JsonSerialization.readValue(content, AccessToken.class);
            resource.setOwner(accessToken.getSubject());

            resource.setName("Resource " + i);
            resource.setDisplayName("Display Name " + i);
            resource.setIconUri("Icon Uri " + i);
            resource.addScope("Scope A", "Scope B", "Scope C", "Scope D");
            resource.setUri("http://resourceServer.com/resources/" + i);

            try (Response response1 = authorization.resources().create(resource)) {
                resource.setId(response1.readEntity(ResourceRepresentation.class).getId());
            }

            for (String scope : Arrays.asList("Scope A", "Scope B")) {
                PermissionTicketRepresentation ticket = new PermissionTicketRepresentation();

                ticket.setGranted(true);
                ticket.setOwner(resource.getOwner().getId());
                ticket.setRequesterName(userNames[i % userNames.length]);
                ticket.setResource(resource.getId());
                ticket.setScopeName(scope);

                authzClient.protection("jdoe", "password").permission().create(ticket);
            }
        }
    }

    @Test
    public void shouldShowMyResourcesInTable() {
        myResourcesPage.assertCurrent();

        assertEquals(10, myResourcesPage.getResourcesListCount());
    }

    private AuthzClient createAuthzClient(ClientRepresentation client) {
        Map<String, Object> credentials = new HashMap<>();

        credentials.put("secret", "secret");

        return AuthzClient
                .create(new Configuration(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth",
                        testRealm.getRealm(), client.getClientId(),
                        credentials, httpClient));
    }

    private ClientResource getResourceServer() {
        ClientsResource clients = adminClient.realm(TEST).clients();
        return clients.get(clients.findByClientId("my-resource-server").get(0).getId());
    }

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return myResourcesPage;
    }
}
