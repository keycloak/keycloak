package org.keycloak.testsuite.admin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.enableDynamicUserProfile;
import static org.keycloak.testsuite.forms.VerifyProfileTest.setUserProfileConfiguration;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ErrorRepresentation;
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
        realmRep.setInternationalizationEnabled(true);
        realmRep.setSupportedLocales(new HashSet<>(Arrays.asList("en", "de")));
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

        user1.setAttributes(Collections.emptyMap());
        verifyUserUpdateFails(user1Id, user1, "Please specify attribute attr1.");
    }

    private void verifyUserUpdateFails(String userId, UserRepresentation user, String expectedErrorMessage) {
        UserResource userResource = realm.users().get(userId);
        try {
            userResource.update(user);
            fail("Should fail with errorMessage: " + expectedErrorMessage);
        } catch (BadRequestException badRequest) {
            try (Response response = badRequest.getResponse()) {
                assertThat(response.getStatus(), equalTo(400));
                ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
                assertThat(error.getErrorMessage(), equalTo(expectedErrorMessage));
            }
        }
    }

    /**
     * TODO:
     * This test shows an unexpected behavior in the Admin UI. The locale used for user validation error messages
     * is the locale of the validated user, but probably it should be the locale of the authenticated user.
     */
    @Test
    public void validationErrorMessagesCanBeConfiguredWithRealmLocalization() {
        String requiredAttrName = "required-attr";
        setUserProfileConfiguration(this.realm, "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"locale\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"" + requiredAttrName + "\", \"required\": {}, " + PERMISSIONS_ALL + "}]}");

        realm.localization().saveRealmLocalizationText("en", "error-user-attribute-required",
                "required-error en: {0}");
        getCleanup().addLocalization("en");
        realm.localization().saveRealmLocalizationText("de", "error-user-attribute-required",
                "required-error de: {0}");
        getCleanup().addLocalization("de");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user-realm-localization");
        // start with locale en
        user.singleAttribute("locale", "en");
        user.singleAttribute(requiredAttrName, "some-value");
        String userId = createUser(user);

        user.setAttributes(new HashMap<>());
        verifyUserUpdateFails(userId, user, "required-error en: " + requiredAttrName);

        // switch to locale de
        user.singleAttribute("locale", "de");
        user.singleAttribute(requiredAttrName, "some-value");
        realm.users().get(userId).update(user);

        user.setAttributes(new HashMap<>());
        verifyUserUpdateFails(userId, user, "required-error de: " + requiredAttrName);
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

    private String createUser(UserRepresentation userRep) {
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addUserId(createdId);
        return createdId;
    }
}
