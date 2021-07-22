package org.keycloak.testsuite.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.enableDynamicUserProfile;
import static org.keycloak.testsuite.forms.VerifyProfileTest.setUserProfileConfiguration;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.REALM_USER_PROFILE_ENABLED;

import javax.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.AdminEventPaths;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeclarativeUserTest extends AbstractAdminTest {

    @Before
    public void onBefore() {
        RealmRepresentation realmRep = this.realm.toRepresentation();
        enableDynamicUserProfile(realmRep);
        this.realm.update(realmRep);
        setUserProfileConfiguration(this.realm, "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"aName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"custom-a\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"custom-hidden\"},"
                + "{\"name\": \"attr1\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\", " + PERMISSIONS_ALL + "}]}");
    }

    @Test
    public void testReturnAllConfiguredAttributesEvenIfNotSet() {
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.singleAttribute("attr1", "value1user1");
        user1.singleAttribute("attr2", "value2user1");
        String user1Id = createUser(user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        Map<String, List<String>> attributes = user1.getAttributes();
        assertEquals(4, attributes.size());
        List<String> attr1 = attributes.get("attr1");
        assertEquals(1, attr1.size());
        assertEquals("value1user1", attr1.get(0));
        List<String> attr2 = attributes.get("attr2");
        assertEquals(1, attr2.size());
        assertEquals("value2user1", attr2.get(0));
        List<String> attrCustomA = attributes.get("custom-a");
        assertTrue(attrCustomA.isEmpty());
        assertTrue(attributes.containsKey("custom-a"));
        assertTrue(attributes.containsKey("aName"));
    }

    @Test
    public void testDoNotReturnAttributeIfNotReadble() {
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.singleAttribute("attr1", "value1user1");
        user1.singleAttribute("attr2", "value2user1");
        String user1Id = createUser(user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        Map<String, List<String>> attributes = user1.getAttributes();
        assertEquals(4, attributes.size());
        assertFalse(attributes.containsKey("custom-hidden"));

        setUserProfileConfiguration(this.realm, "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"aName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"custom-a\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"custom-hidden\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr1\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\", " + PERMISSIONS_ALL + "}]}");


        user1 = realm.users().get(user1Id).toRepresentation();
        attributes = user1.getAttributes();
        assertEquals(5, attributes.size());
        assertTrue(attributes.containsKey("custom-hidden"));
    }

    private String createUser(UserRepresentation userRep) {
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addUserId(createdId);
        return createdId;
    }
}
