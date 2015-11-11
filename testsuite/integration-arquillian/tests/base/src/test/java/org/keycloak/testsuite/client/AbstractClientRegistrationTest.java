package org.keycloak.testsuite.client;

import org.junit.After;
import org.junit.Before;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractClientRegistrationTest extends AbstractKeycloakTest {

    static final String REALM_NAME = "test";

    ClientRegistration reg;

    @Before
    public void before() throws Exception {
        reg = new ClientRegistration(testContext.getAuthServerContextRoot() + "/auth", "test");
    }

    @After
    public void after() throws Exception {
        reg.close();
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

    public ClientRepresentation createClient(ClientRepresentation client) {
        Response response = adminClient.realm(REALM_NAME).clients().create(client);
        String id = response.getLocation().toString();
        id = id.substring(id.lastIndexOf('/') + 1);
        client.setId(id);
        response.close();
        return client;
    }

    public ClientRepresentation getClient(String clientId) {
        try {
            return adminClient.realm(REALM_NAME).clients().get(clientId).toRepresentation();
        } catch (NotFoundException e) {
            return null;
        }
    }

}
