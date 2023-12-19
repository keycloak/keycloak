package org.keycloak.testsuite.admin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;
import static org.keycloak.testsuite.forms.VerifyProfileTest.setUserProfileConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.UserProfileProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeclarativeUserTest extends AbstractAdminTest {

    private static final String TEST_REALM_USER_MANAGER_NAME = "test-realm-user-manager";
    private static final String REQUIRED_ATTR_KEY = "required-attr";

    private Keycloak testRealmUserManagerClient;

    @Before
    public void onBefore() throws Exception {
        RealmRepresentation realmRep = realm.toRepresentation();
        realmRep.setInternationalizationEnabled(true);
        realmRep.setSupportedLocales(new HashSet<>(Arrays.asList("en", "de")));
        realm.update(realmRep);
        setUserProfileConfiguration(realm, "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"aName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"custom-a\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"custom-hidden\"},"
                + "{\"name\": \"attr1\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\", " + PERMISSIONS_ALL + "}]}");

        UserRepresentation testRealmUserManager = UserBuilder.create().username(TEST_REALM_USER_MANAGER_NAME)
                .password(TEST_REALM_USER_MANAGER_NAME).build();
        String createdUserId = null;
        try (Response response = realm.users().create(testRealmUserManager)) {
            createdUserId = ApiUtil.getCreatedId(response);
        } catch (WebApplicationException e) {
            // it's ok when the user has already been created for a previous test
            assertThat(e.getResponse().getStatus(), equalTo(409));
        }

        if (createdUserId != null) {
            List<ClientRepresentation> foundClients = realm.clients().findByClientId("realm-management");
            assertThat(foundClients, hasSize(1));
            ClientRepresentation realmManagementClient = foundClients.get(0);

            RoleRepresentation manageUsersRole =
                    realm.clients().get(realmManagementClient.getId()).roles().get("manage-users").toRepresentation();
            assertThat(manageUsersRole, notNullValue());

            realm.users().get(createdUserId).roles().clientLevel(realmManagementClient.getId())
                    .add(Collections.singletonList(manageUsersRole));
        }

        ClientRepresentation testApp = new ClientRepresentation();
        testApp.setClientId("test-app");
        testApp.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        testApp.setSecret("secret");
        try (Response response = realm.clients().create(testApp)) {
            ApiUtil.getCreatedId(response);
        } catch (WebApplicationException e) {
            // it's ok when the client has already been created for a previous test
        }

        testRealmUserManagerClient = AdminClientUtil.createAdminClient(true, realmRep.getRealm(),
                TEST_REALM_USER_MANAGER_NAME, TEST_REALM_USER_MANAGER_NAME, testApp.getClientId(), testApp.getSecret());
    }

    @After
    public void closeClient() {
        if (testRealmUserManagerClient != null) {
            testRealmUserManagerClient.close();
        }
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
        assertEquals(2, attributes.size());
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
        assertEquals(2, attributes.size());
        assertFalse(attributes.containsKey("custom-hidden"));
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
        assertNull(user1.getAttributes());
        user1.singleAttribute("attr2", "");
        // should be able to update the user when a read-only attribute has an empty or null value
        userResource.update(user1);
        user1 = userResource.toRepresentation();
        assertNull(user1.getAttributes());
        user1.setAttributes(new HashMap<>());
        user1.getAttributes().put("attr2", null);
        userResource.update(user1);
        user1 = userResource.toRepresentation();
        assertNull(user1.getAttributes());
    }

    @Test
    public void testValidationUsingExistingAttributes() {
        setUserProfileConfiguration(this.realm, "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"" + REQUIRED_ATTR_KEY + "\", \"required\": {}, " + PERMISSIONS_ALL + "}]}");

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        // set an attribute to later remove it from the configuration
        user1.singleAttribute(REQUIRED_ATTR_KEY, "some-value");
        String user1Id = createUser(user1);

        UserResource userResource = realm.users().get(user1Id);
        user1 = userResource.toRepresentation();
        user1.setFirstName("changed");
        user1.setAttributes(null);

        // do not validate REQUIRED_ATTR_KEY because the attribute list is not provided and the user has the attribute
        userResource.update(user1);
        user1 = userResource.toRepresentation();
        assertEquals("changed", user1.getFirstName());

        user1.setAttributes(Collections.emptyMap());
        String expectedErrorMessage = "error-user-attribute-required";
        verifyUserUpdateFails(realm.users(), user1Id, user1, expectedErrorMessage);
    }

    private void verifyUserUpdateFails(UsersResource usersResource, String userId, UserRepresentation user,
            String expectedErrorMessage) {
        UserResource userResource = usersResource.get(userId);
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

    @Test
    public void testDefaultUserProfileProviderIsActive() {
        getTestingClient().server(REALM_NAME).run(session -> {
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
    public void testUserLocale() {
        RealmRepresentation realmRep = realm.toRepresentation();
        Boolean internationalizationEnabled = realmRep.isInternationalizationEnabled();
        realmRep.setInternationalizationEnabled(true);
        realm.update(realmRep);

        try {
            UserRepresentation user1 = new UserRepresentation();
            user1.setUsername("user1");
            user1.singleAttribute(UserModel.LOCALE, "pt_BR");
            String user1Id = createUser(user1);

            UserResource userResource = realm.users().get(user1Id);
            user1 = userResource.toRepresentation();
            assertEquals("pt_BR", user1.getAttributes().get(UserModel.LOCALE).get(0));

            realmRep.setInternationalizationEnabled(false);
            realm.update(realmRep);

            user1 = userResource.toRepresentation();
            assertNull(user1.getAttributes());
        } finally {
            realmRep.setInternationalizationEnabled(internationalizationEnabled);
            realm.update(realmRep);
        }
    }

    private String createUser(UserRepresentation userRep) {
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addUserId(createdId);
        return createdId;
    }
}
