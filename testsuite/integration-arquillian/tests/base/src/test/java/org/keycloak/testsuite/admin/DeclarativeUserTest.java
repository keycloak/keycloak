package org.keycloak.testsuite.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_READABLE;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_USER_ONLY;
import static org.keycloak.testsuite.forms.VerifyProfileTest.enableDynamicUserProfile;
import static org.keycloak.testsuite.forms.VerifyProfileTest.setUserProfileConfiguration;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.account.UserProfileAttributeMetadata;
import org.keycloak.representations.account.UserProfileMetadata;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
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

    @Test
    public void testUpdateUnsetAttributeWithEmptyValue() {
        setUserProfileConfiguration(this.realm, "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr1\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\"}]}");

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        // set an attribute to later remove it from the configuration
        user1.singleAttribute("attr1", "some-value");
        String user1Id = createUser(user1);

        // remove the attr1 attribute from the configuration
        setUserProfileConfiguration(this.realm, "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\"}]}");

        UserResource userResource = realm.users().get(user1Id);
        user1 = userResource.toRepresentation();
        Map<String, List<String>> attributes = user1.getAttributes();
        attributes.put("attr2", Collections.singletonList(""));
        // should be able to update the user when a read-only attribute has an empty or null value
        userResource.update(user1);
        attributes.put("attr2", null);
        userResource.update(user1);
    }

    @Test
    public void testValidationUsingExistingAttributes() {
        setUserProfileConfiguration(this.realm, "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr1\", \"required\": {}, " + PERMISSIONS_ALL + "}]}");

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        // set an attribute to later remove it from the configuration
        user1.singleAttribute("attr1", "some-value");
        String user1Id = createUser(user1);

        UserResource userResource = realm.users().get(user1Id);
        user1 = userResource.toRepresentation();
        user1.setFirstName("changed");
        user1.setAttributes(null);

        // do not validate attr1 because the attribute list is not provided and the user has the attribute
        userResource.update(user1);
        user1 = userResource.toRepresentation();
        assertEquals("changed", user1.getFirstName());

        try {
            user1.setAttributes(Collections.emptyMap());
            userResource.update(user1);
            fail("Should fail because the attribute attr1 is required");
        } catch (BadRequestException ignore) {

        }
    }

    @Test
    public void testDefaultUserProfileProviderIsActive() {
        getTestingClient().server(TEST_REALM_NAME).run(session -> {
            Set<UserProfileProvider> providers = session.getAllProviders(UserProfileProvider.class);
            assertThat(providers, notNullValue());
            assertThat(providers.isEmpty(), is(false));

            UserProfileProvider provider = session.getProvider(UserProfileProvider.class);
            assertThat(provider, notNullValue());
            assertThat(DeclarativeUserProfileProvider.class.getName(), is(provider.getClass().getName()));
            assertThat(provider, instanceOf(DeclarativeUserProfileProvider.class));
        });
    }

    @Test
    public void testGetAttributeMetadata() {
        setUserProfileConfiguration(this.realm, "{\"attributes\": ["
                + "{\"name\": \"firstName\", " + PERMISSIONS_ADMIN_READABLE + ", \"required\": {}},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ADMIN_READABLE + "},"
                + "{\"name\": \"attr1\", " + PERMISSIONS_USER_ONLY + "},"
                + "{\"name\": \"attr2\", " + PERMISSIONS_ALL + ", \"validations\": {"
                + "\"length\": { \"max\": 255 },"
                + "\"person-name-prohibited-characters\": {}"
                + "}, \"annotations\": {\"anno1\": \"value1\",\"anno2\": \"value2\"}}]}");

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        // set an attribute to later remove it from the configuration
        user1.singleAttribute("attr1", "some-value");
        String user1Id = createUser(user1);
        UserResource userResource = realm.users().get(user1Id);
        user1 = userResource.toRepresentation();
        UserProfileMetadata attributesMetadata = user1.getAttributesMetadata();

        // do not return attribute metadata
        assertNull(attributesMetadata);

        user1 = userResource.toRepresentation(true);
        attributesMetadata = user1.getAttributesMetadata();

        assertNotNull(attributesMetadata);

        UserProfileAttributeMetadata firstName = attributesMetadata.getAttributeMetadata("firstName");

        assertNotNull(firstName);
        assertTrue(firstName.isRequired());
        assertTrue(firstName.isReadOnly());
        assertTrue(firstName.isRequired());
        assertTrue(firstName.getValidators().isEmpty());

        UserProfileAttributeMetadata lastName = attributesMetadata.getAttributeMetadata("lastName");

        assertNotNull(lastName);
        assertTrue(lastName.isReadOnly());
        assertFalse(lastName.isRequired());
        assertTrue(firstName.getValidators().isEmpty());

        UserProfileAttributeMetadata attr1 = attributesMetadata.getAttributeMetadata("attr1");

        // do not return metadata if attribute is not readable
        assertNull(attr1);

        UserProfileAttributeMetadata attr2 = attributesMetadata.getAttributeMetadata("attr2");

        assertNotNull(attr2);
        assertFalse(attr2.isReadOnly());
        Map<String, Map<String, Object>> validators = attr2.getValidators();
        assertFalse(validators.isEmpty());
        assertEquals(2, validators.size());
        Map<String, Object> validatorConfig = validators.get("length");
        assertNotNull(validatorConfig);
        assertEquals(255, validatorConfig.get("max"));
        Map<String, Object> annotations = attr2.getAnnotations();
        assertNotNull(annotations);
        assertEquals(2, annotations.size());
        assertEquals("value1", annotations.get("anno1"));
        assertEquals("value2", annotations.get("anno2"));
    }

    private String createUser(UserRepresentation userRep) {
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addUserId(createdId);
        return createdId;
    }
}
