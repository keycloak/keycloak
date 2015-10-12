package org.keycloak.testsuite.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientRegistrationTest extends AbstractKeycloakTest {

    private static final String REALM_NAME = "test";
    private static final String CLIENT_ID = "test-client";
    private static final String CLIENT_SECRET = "test-client-secret";

    private ClientRegistration clientRegistrationAsAdmin;
    private ClientRegistration clientRegistrationAsClient;

    @Before
    public void before() throws ClientRegistrationException {
        clientRegistrationAsAdmin = clientBuilder().auth(getToken("manage-clients", "password")).build();
        clientRegistrationAsClient = clientBuilder().auth(CLIENT_ID, CLIENT_SECRET).build();
    }

    @After
    public void after() throws ClientRegistrationException {
        clientRegistrationAsAdmin.close();
        clientRegistrationAsClient.close();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setEnabled(true);
        rep.setRealm(REALM_NAME);
        rep.setUsers(new LinkedList<UserRepresentation>());

        LinkedList<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        credentials.add(password);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("manage-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.MANAGE_CLIENTS)));

        rep.getUsers().add(user);

        UserRepresentation user2 = new UserRepresentation();
        user2.setEnabled(true);
        user2.setUsername("create-clients");
        user2.setCredentials(credentials);
        user2.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.CREATE_CLIENT)));

        rep.getUsers().add(user2);

        UserRepresentation user3 = new UserRepresentation();
        user3.setEnabled(true);
        user3.setUsername("no-access");
        user3.setCredentials(credentials);

        rep.getUsers().add(user3);

        testRealms.add(rep);
    }

    private void registerClient(ClientRegistration clientRegistration) throws ClientRegistrationException {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        ClientRepresentation createdClient = clientRegistration.create(client);
        assertEquals(CLIENT_ID, createdClient.getClientId());

        client = adminClient.realm(REALM_NAME).clients().get(createdClient.getId()).toRepresentation();
        assertEquals(CLIENT_ID, client.getClientId());

        AccessTokenResponse token2 = oauthClient.getToken(REALM_NAME, CLIENT_ID, CLIENT_SECRET, "manage-clients", "password");
        assertNotNull(token2.getToken());
    }

    @Test
    public void registerClientAsAdmin() throws ClientRegistrationException {
        registerClient(clientRegistrationAsAdmin);
    }

    @Test
    public void registerClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        ClientRegistration clientRegistration = clientBuilder().auth(getToken("create-clients", "password")).build();
        try {
            registerClient(clientRegistration);
        } finally {
            clientRegistration.close();
        }
    }

    @Test
    public void registerClientAsAdminWithNoAccess() throws ClientRegistrationException {
        ClientRegistration clientRegistration = clientBuilder().auth(getToken("no-access", "password")).build();
        try {
            registerClient(clientRegistration);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        } finally {
            clientRegistration.close();
        }
    }

    @Test
    public void getClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        registerClient(clientRegistrationAsAdmin);
        ClientRegistration clientRegistration = clientBuilder().auth(getToken("create-clients", "password")).build();
        try {
            clientRegistration.get(CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        } finally {
            clientRegistration.close();
        }
    }

    @Test
    public void wrongClient() throws ClientRegistrationException {
        registerClient(clientRegistrationAsAdmin);

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("test-client-2");
        client.setSecret("test-client-2-secret");

        clientRegistrationAsAdmin.create(client);

        ClientRegistration clientRegistration = clientBuilder().auth("test-client-2", "test-client-2-secret").build();

        client = clientRegistration.get("test-client-2");
        assertNotNull(client);
        assertEquals("test-client-2", client.getClientId());

        try {
            try {
                clientRegistration.get(CLIENT_ID);
                fail("Expected 403");
            } catch (ClientRegistrationException e) {
                assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
            }

            client = clientRegistrationAsAdmin.get(CLIENT_ID);
            try {
                clientRegistration.update(client);
                fail("Expected 403");
            } catch (ClientRegistrationException e) {
                assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
            }

            try {
                clientRegistration.delete(CLIENT_ID);
                fail("Expected 403");
            } catch (ClientRegistrationException e) {
                assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
            }
        }
        finally {
            clientRegistration.close();
        }
    }

    @Test
    public void getClientAsAdminWithNoAccess() throws ClientRegistrationException {
        registerClient(clientRegistrationAsAdmin);
        ClientRegistration clientRegistration = clientBuilder().auth(getToken("no-access", "password")).build();
        try {
            clientRegistration.get(CLIENT_ID);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        } finally {
            clientRegistration.close();
        }
    }

    private void updateClient(ClientRegistration clientRegistration) throws ClientRegistrationException {
        ClientRepresentation client = clientRegistration.get(CLIENT_ID);
        client.setRedirectUris(Collections.singletonList("http://localhost:8080/app"));

        clientRegistration.update(client);

        ClientRepresentation updatedClient = clientRegistration.get(CLIENT_ID);

        assertEquals(1, updatedClient.getRedirectUris().size());
        assertEquals("http://localhost:8080/app", updatedClient.getRedirectUris().get(0));
    }

    @Test
    public void updateClientAsAdmin() throws ClientRegistrationException {
        registerClient(clientRegistrationAsAdmin);
        updateClient(clientRegistrationAsAdmin);
    }

    @Test
    public void updateClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        ClientRegistration clientRegistration = clientBuilder().auth(getToken("create-clients", "password")).build();
        try {
            updateClient(clientRegistration);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        } finally {
            clientRegistration.close();
        }
    }

    @Test
    public void updateClientAsAdminWithNoAccess() throws ClientRegistrationException {
        ClientRegistration clientRegistration = clientBuilder().auth(getToken("no-access", "password")).build();
        try {
            updateClient(clientRegistration);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        } finally {
            clientRegistration.close();
        }
    }

    @Test
    public void updateClientAsClient() throws ClientRegistrationException {
        registerClient(clientRegistrationAsAdmin);
        updateClient(clientRegistrationAsClient);
    }

    private void deleteClient(ClientRegistration clientRegistration) throws ClientRegistrationException {
        clientRegistration.delete(CLIENT_ID);

        // Can't authenticate as client after client is deleted
        ClientRepresentation client = clientRegistrationAsAdmin.get(CLIENT_ID);
        assertNull(client);
    }

    @Test
    public void deleteClientAsAdmin() throws ClientRegistrationException {
        registerClient(clientRegistrationAsAdmin);
        deleteClient(clientRegistrationAsAdmin);
    }

    @Test
    public void deleteClientAsAdminWithCreateOnly() throws ClientRegistrationException {
        ClientRegistration clientRegistration = clientBuilder().auth(getToken("create-clients", "password")).build();
        try {
            deleteClient(clientRegistration);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        } finally {
            clientRegistration.close();
        }
    }

    @Test
    public void deleteClientAsAdminWithNoAccess() throws ClientRegistrationException {
        ClientRegistration clientRegistration = clientBuilder().auth(getToken("no-access", "password")).build();
        try {
            deleteClient(clientRegistration);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        } finally {
            clientRegistration.close();
        }
    }

    @Test
    public void deleteClientAsClient() throws ClientRegistrationException {
        registerClient(clientRegistrationAsAdmin);
        deleteClient(clientRegistrationAsClient);
    }

    private ClientRegistration.ClientRegistrationBuilder clientBuilder() {
        return ClientRegistration.create().realm("test").authServerUrl(testContext.getAuthServerContextRoot() + "/auth");
    }

    private String getToken(String username, String password) {
        return oauthClient.getToken(REALM_NAME, "security-admin-console", null, username, password).getToken();
    }

}
