package org.keycloak.testsuite.admin;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserTest extends AbstractClientTest {

    @Test
    public void createUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        user.setEmail("user1@localhost");

        realm.users().create(user);
    }

    @Test
    public void createDuplicatedUser() {
        createUser();

        try {
            UserRepresentation user = new UserRepresentation();
            user.setUsername("user1");
            realm.users().create(user);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(409, e.getResponse().getStatus());
        }
    }

    private void createUsers() {
        for (int i = 1; i < 10; i++) {
            UserRepresentation user = new UserRepresentation();
            user.setUsername("username" + i);
            user.setEmail("user" + i + "@localhost");
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);

            realm.users().create(user);
        }
    }

    @Test
    public void searchByEmail() {
        createUsers();

        List<UserRepresentation> users = realm.users().search(null, null, null, "user1@localhost", null, null);
        assertEquals(1, users.size());

        users = realm.users().search(null, null, null, "@localhost", null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void searchByUsername() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username1", null, null, null, null, null);
        assertEquals(1, users.size());

        users = realm.users().search("user", null, null, null, null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void search() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username1", null, null);
        assertEquals(1, users.size());

        users = realm.users().search("first1", null, null);
        assertEquals(1, users.size());

        users = realm.users().search("last", null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void searchPaginated() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username", 0, 1);
        assertEquals(1, users.size());
        assertEquals("username1", users.get(0).getUsername());

        users = realm.users().search("username", 5, 2);
        assertEquals(2, users.size());
        assertEquals("username6", users.get(0).getUsername());
        assertEquals("username7", users.get(1).getUsername());

        users = realm.users().search("username", 7, 20);
        assertEquals(2, users.size());
        assertEquals("username8", users.get(0).getUsername());
        assertEquals("username9", users.get(1).getUsername());

        users = realm.users().search("username", 0, 20);
        assertEquals(9, users.size());
    }

    @Test
    public void addSocialLink() {
        createUser();

        UserResource user = realm.users().get("user1");

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("social-user-id");
        link.setUserName("social-username");

        Response response = user.addSocialLink("social-provider-id", link);
        assertEquals(204, response.getStatus());
    }

    @Test
    @Ignore("Refactor based on KEYCLOAK-883")
    public void getSocialLinks() {
        addSocialLink();

        UserResource user = realm.users().get("user1");
        assertEquals(1, user.getSocialLinks().size());

        FederatedIdentityRepresentation link = user.getSocialLinks().get(0);
        assertEquals("social-provider-id", link.getIdentityProvider());
        assertEquals("social-user-id", link.getUserId());
        assertEquals("social-username", link.getUserName());
    }

    @Test
    @Ignore("Refactor based on KEYCLOAK-883")
    public void removeSocialLink() {
        addSocialLink();

        UserResource user = realm.users().get("user1");
        assertEquals(1, user.getSocialLinks().size());

        user.removeSocialLink("social-provider-id");

        assertEquals(0, user.getSocialLinks().size());
    }

    @Test
    public void addRequiredAction() {
        createUser();

        UserResource user = realm.users().get("user1");
        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());

        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add("UPDATE_PASSWORD");
        user.update(userRep);

        assertEquals(1, user.toRepresentation().getRequiredActions().size());
        assertEquals("UPDATE_PASSWORD", user.toRepresentation().getRequiredActions().get(0));
    }

    @Test
    public void removeRequiredAction() {
        addRequiredAction();

        UserResource user = realm.users().get("user1");
        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().clear();
        user.update(userRep);

        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());
    }

    @Test
    public void attributes() {
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.attribute("attr1", "value1user1");
        user1.attribute("attr2", "value2user1");
        realm.users().create(user1);

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("user2");
        user2.attribute("attr1", "value1user2");
        user2.attribute("attr2", "value2user2");
        realm.users().create(user2);

        user1 = realm.users().get("user1").toRepresentation();
        assertEquals(2, user1.getAttributes().size());
        assertEquals("value1user1", user1.getAttributes().get("attr1"));
        assertEquals("value2user1", user1.getAttributes().get("attr2"));

        user2 = realm.users().get("user2").toRepresentation();
        assertEquals(2, user2.getAttributes().size());
        assertEquals("value1user2", user2.getAttributes().get("attr1"));
        assertEquals("value2user2", user2.getAttributes().get("attr2"));

        user1.attribute("attr1", "value3user1");
        user1.attribute("attr3", "value4user1");

        realm.users().get("user1").update(user1);

        user1 = realm.users().get("user1").toRepresentation();
        assertEquals(3, user1.getAttributes().size());
        assertEquals("value3user1", user1.getAttributes().get("attr1"));
        assertEquals("value2user1", user1.getAttributes().get("attr2"));
        assertEquals("value4user1", user1.getAttributes().get("attr3"));

        user1.getAttributes().remove("attr1");
        realm.users().get("user1").update(user1);

        user1 = realm.users().get("user1").toRepresentation();
        assertEquals(2, user1.getAttributes().size());
        assertEquals("value2user1", user1.getAttributes().get("attr2"));
        assertEquals("value4user1", user1.getAttributes().get("attr3"));

        user1.getAttributes().clear();
        realm.users().get("user1").update(user1);

        user1 = realm.users().get("user1").toRepresentation();
        assertNull(user1.getAttributes());
    }

}
