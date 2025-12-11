package org.keycloak.tests.admin.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = UserAttributesTest.UserAttributesServerConfig.class)
public class UserAttributesTest extends AbstractUserTest {

    @Test
    public void countByAttribute() {
        createUsers();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("test1", "test2");
        assertThat(managedRealm.admin().users().count(null, null, null, null, null, null, null, mapToSearchQuery(attributes)), is(0));

        attributes = new HashMap<>();
        attributes.put("test", "test1");
        assertThat(managedRealm.admin().users().count(null, null, null, null, null, null, null, mapToSearchQuery(attributes)), is(1));

        attributes = new HashMap<>();
        attributes.put("test", "test2");
        attributes.put("attr", "common");
        assertThat(managedRealm.admin().users().count(null, null, null, null, null, null, null, mapToSearchQuery(attributes)), is(1));

        attributes = new HashMap<>();
        attributes.put("attr", "common");
        assertThat(managedRealm.admin().users().count(null, null, null, null, null, null, null, mapToSearchQuery(attributes)), is(9));

        attributes = new HashMap<>();
        attributes.put("attr", "common");
        attributes.put(UserModel.EXACT, Boolean.FALSE.toString());
        assertThat(managedRealm.admin().users().count(null, null, null, null, null, null, null, mapToSearchQuery(attributes)), is(9));
    }

    @Test
    public void attributes() {
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.singleAttribute("attr1", "value1user1");
        user1.singleAttribute("attr2", "value2user1");

        String user1Id = createUser(user1);

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("user2");
        user2.singleAttribute("attr1", "value1user2");
        List<String> vals = new ArrayList<>();
        vals.add("value2user2");
        vals.add("value2user2_2");
        user2.getAttributes().put("attr2", vals);

        String user2Id = createUser(user2);

        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        assertEquals(2, user1.getAttributes().size());
        assertAttributeValue("value1user1", user1.getAttributes().get("attr1"));
        assertAttributeValue("value2user1", user1.getAttributes().get("attr2"));

        user2 = managedRealm.admin().users().get(user2Id).toRepresentation();
        assertEquals(2, user2.getAttributes().size());
        assertAttributeValue("value1user2", user2.getAttributes().get("attr1"));
        vals = user2.getAttributes().get("attr2");
        assertEquals(2, vals.size());
        assertTrue(vals.contains("value2user2") && vals.contains("value2user2_2"));

        user1.singleAttribute("attr1", "value3user1");
        user1.singleAttribute("attr3", "value4user1");

        updateUser(managedRealm.admin().users().get(user1Id), user1);

        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        assertEquals(3, user1.getAttributes().size());
        assertAttributeValue("value3user1", user1.getAttributes().get("attr1"));
        assertAttributeValue("value2user1", user1.getAttributes().get("attr2"));
        assertAttributeValue("value4user1", user1.getAttributes().get("attr3"));

        user1.getAttributes().remove("attr1");
        updateUser(managedRealm.admin().users().get(user1Id), user1);

        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        assertEquals(2, user1.getAttributes().size());
        assertAttributeValue("value2user1", user1.getAttributes().get("attr2"));
        assertAttributeValue("value4user1", user1.getAttributes().get("attr3"));

        // null attributes should not remove attributes
        user1.setAttributes(null);
        updateUser(managedRealm.admin().users().get(user1Id), user1);
        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        assertNotNull(user1.getAttributes());
        assertEquals(2, user1.getAttributes().size());

        // empty attributes should remove attributes
        user1.setAttributes(Collections.emptyMap());
        updateUser(managedRealm.admin().users().get(user1Id), user1);

        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        assertNull(user1.getAttributes());

        Map<String, List<String>> attributes = new HashMap<>();

        attributes.put("foo", List.of("foo"));
        attributes.put("bar", List.of("bar"));

        user1.setAttributes(attributes);

        managedRealm.admin().users().get(user1Id).update(user1);
        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        assertEquals(2, user1.getAttributes().size());

        user1.getAttributes().remove("foo");

        managedRealm.admin().users().get(user1Id).update(user1);
        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        assertEquals(1, user1.getAttributes().size());
    }

    @Test
    public void updateUserWithReadOnlyAttributes() {
        // Admin is able to update "usercertificate" attribute
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.singleAttribute("usercertificate", "foo1");
        String user1Id = createUser(user1);
        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();

        // Update of the user should be rejected due adding the "denied" attribute LDAP_ID
        try {
            user1.singleAttribute("usercertificate", "foo");
            user1.singleAttribute("saml.persistent.name.id.for.foo", "bar");
            user1.singleAttribute(LDAPConstants.LDAP_ID, "baz");
            updateUser(managedRealm.admin().users().get(user1Id), user1);
            Assertions.fail("Not supposed to successfully update user");
        } catch (BadRequestException expected) {
            // Expected
            Assertions.assertNull(adminEvents.poll());
            ErrorRepresentation error = expected.getResponse().readEntity(ErrorRepresentation.class);
            Assertions.assertEquals("updateReadOnlyAttributesRejectedMessage", error.getErrorMessage());
        }

        // The same test as before, but with the case-sensitivity used
        try {
            user1.getAttributes().remove(LDAPConstants.LDAP_ID);
            user1.singleAttribute("LDap_Id", "baz");
            updateUser(managedRealm.admin().users().get(user1Id), user1);
            Assertions.fail("Not supposed to successfully update user");
        } catch (BadRequestException bre) {
            // Expected
            Assertions.assertNull(adminEvents.poll());
        }

        // Attribute "deniedSomeAdmin" was denied for administrator
        try {
            user1.getAttributes().remove("LDap_Id");
            user1.singleAttribute("deniedSomeAdmin", "baz");
            updateUser(managedRealm.admin().users().get(user1Id), user1);
            Assertions.fail("Not supposed to successfully update user");
        } catch (BadRequestException bre) {
            // Expected
            Assertions.assertNull(adminEvents.poll());
        }

        // usercertificate and saml attribute are allowed by admin
        user1.getAttributes().remove("deniedSomeAdmin");
        updateUser(managedRealm.admin().users().get(user1Id), user1);

        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        assertEquals("foo", user1.getAttributes().get("usercertificate").get(0));
        assertEquals("bar", user1.getAttributes().get("saml.persistent.name.id.for.foo").get(0));
        assertFalse(user1.getAttributes().containsKey(LDAPConstants.LDAP_ID));
    }

    @Test
    public void testImportUserWithNullAttribute() {
        RealmRepresentation rep = loadJson(UserAttributesTest.class.getResourceAsStream("testrealm-user-null-attr.json"), RealmRepresentation.class);

        adminClient.realms().create(rep);
        List<UserRepresentation> users = adminClient.realms().realm("test-user-null-attr").users().list();
        // there should be only one user
        assertThat(users, hasSize(1));
        // test there are only 2 attributes imported from json file, attribute "key3" : [ null ] shouldn't be imported
        assertThat(users.get(0).getAttributes().size(), equalTo(2));
    }

    @Test
    public void testKeepRootAttributeWhenOtherAttributesAreSet() {
        String random = UUID.randomUUID().toString();
        String userName = String.format("username-%s", random);
        String email = String.format("my@mail-%s.com", random);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(userName);
        user.setEmail(email);
        String userId = createUser(user);

        UserRepresentation created = managedRealm.admin().users().get(userId).toRepresentation();
        assertThat(created.getEmail(), equalTo(email));
        assertThat(created.getUsername(), equalTo(userName));
        assertThat(created.getAttributes(), Matchers.nullValue());

        UserRepresentation update = new UserRepresentation();
        update.setId(userId);
        // user profile requires sending all attributes otherwise they are removed
        update.setEmail(email);

        update.setAttributes(Map.of("phoneNumber", List.of("123")));
        updateUser(managedRealm.admin().users().get(userId), update);

        UserRepresentation updated = managedRealm.admin().users().get(userId).toRepresentation();
        assertThat(updated.getUsername(), equalTo(userName));
        assertThat(updated.getAttributes().get("phoneNumber"), equalTo(List.of("123")));

        assertThat(updated.getEmail(), equalTo(email));
    }

    private void assertAttributeValue(String expectedValue, List<String> attrValues) {
        Assertions.assertEquals(1, attrValues.size());
        Assertions.assertEquals(expectedValue, attrValues.get(0));
    }

    public static class UserAttributesServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            builder.option("spi-user-profile-declarative-user-profile-admin-read-only-attributes", "deniedSomeAdmin");

            return builder;
        }
    }
}
