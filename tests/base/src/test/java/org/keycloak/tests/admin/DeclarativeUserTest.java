package org.keycloak.tests.admin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.UserProfileProvider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.PERMISSIONS_ALL;
import static org.keycloak.testsuite.util.userprofile.UserProfileUtil.setUserProfileConfiguration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@KeycloakIntegrationTest
public class DeclarativeUserTest {

    @InjectRealm(config = DeclarativeRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private static final String REQUIRED_ATTR_KEY = "required-attr";

    @BeforeEach
    public void onBefore() {
        setUserProfileConfiguration(managedRealm.admin(), "{\"attributes\": ["
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
    public void testDoNotReturnAttributeIfNotReadble() {
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.singleAttribute("attr1", "value1user1");
        user1.singleAttribute("attr2", "value2user1");
        String user1Id = createUserWithCleanup(user1);

        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        Map<String, List<String>> attributes = user1.getAttributes();
        Assertions.assertEquals(2, attributes.size());
        Assertions.assertFalse(attributes.containsKey("custom-hidden"));

        setUserProfileConfiguration(managedRealm.admin(), "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"aName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"custom-a\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"custom-hidden\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr1\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\", " + PERMISSIONS_ALL + "}]}");


        user1 = managedRealm.admin().users().get(user1Id).toRepresentation();
        attributes = user1.getAttributes();
        Assertions.assertEquals(2, attributes.size());
        Assertions.assertFalse(attributes.containsKey("custom-hidden"));
    }

    @Test
    public void testUpdateUnsetAttributeWithEmptyValue() {
        setUserProfileConfiguration(managedRealm.admin(), "{\"attributes\": ["
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
        String user1Id = createUserWithCleanup(user1);

        // remove the attr1 attribute from the configuration
        setUserProfileConfiguration(managedRealm.admin(), "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"attr2\"}]}");

        UserResource userResource = managedRealm.admin().users().get(user1Id);
        user1 = userResource.toRepresentation();
        Assertions.assertNull(user1.getAttributes());
        user1.singleAttribute("attr2", "");
        // should be able to update the user when a read-only attribute has an empty or null value
        userResource.update(user1);
        user1 = userResource.toRepresentation();
        Assertions.assertNull(user1.getAttributes());
        user1.setAttributes(new HashMap<>());
        user1.getAttributes().put("attr2", null);
        userResource.update(user1);
        user1 = userResource.toRepresentation();
        Assertions.assertNull(user1.getAttributes());
    }

    @Test
    public void testValidationUsingExistingAttributes() {
        setUserProfileConfiguration(managedRealm.admin(), "{\"attributes\": ["
                + "{\"name\": \"username\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"email\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\", " + PERMISSIONS_ALL + "},"
                + "{\"name\": \"" + REQUIRED_ATTR_KEY + "\", \"required\": {}, " + PERMISSIONS_ALL + "}]}");

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        // set an attribute to later remove it from the configuration
        user1.singleAttribute(REQUIRED_ATTR_KEY, "some-value");
        String user1Id = createUserWithCleanup(user1);

        UserResource userResource = managedRealm.admin().users().get(user1Id);
        user1 = userResource.toRepresentation();
        user1.setFirstName("changed");
        user1.setAttributes(null);

        // do not validate REQUIRED_ATTR_KEY because the attribute list is not provided and the user has the attribute
        userResource.update(user1);
        user1 = userResource.toRepresentation();
        Assertions.assertEquals("changed", user1.getFirstName());

        user1.setAttributes(Collections.emptyMap());
        String expectedErrorMessage = "error-user-attribute-required";
        verifyUserUpdateFails(managedRealm.admin().users(), user1Id, user1, expectedErrorMessage);
    }

    private void verifyUserUpdateFails(UsersResource usersResource, String userId, UserRepresentation user,
            String expectedErrorMessage) {
        UserResource userResource = usersResource.get(userId);
        try {
            userResource.update(user);
            Assertions.fail("Should fail with errorMessage: " + expectedErrorMessage);
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
        runOnServer.run(session -> {
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
        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        Boolean internationalizationEnabled = realmRep.isInternationalizationEnabled();
        realmRep.setInternationalizationEnabled(true);
        managedRealm.admin().update(realmRep);

        try {
            UserRepresentation user1 = new UserRepresentation();
            user1.setUsername("user1");
            user1.singleAttribute(UserModel.LOCALE, "pt_BR");
            String user1Id = createUserWithCleanup(user1);

            UserResource userResource = managedRealm.admin().users().get(user1Id);
            user1 = userResource.toRepresentation();
            Assertions.assertEquals("pt_BR", user1.getAttributes().get(UserModel.LOCALE).get(0));

            realmRep.setInternationalizationEnabled(false);
            managedRealm.admin().update(realmRep);

            user1 = userResource.toRepresentation();
            Assertions.assertNull(user1.getAttributes());
        } finally {
            realmRep.setInternationalizationEnabled(internationalizationEnabled);
            managedRealm.admin().update(realmRep);
        }
    }

    private String createUserWithCleanup(UserRepresentation userRep) {
        Response response = managedRealm.admin().users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        managedRealm.cleanup().add(r -> r.users().get(createdId).remove());

        return createdId;
    }

    public static class DeclarativeRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.internationalizationEnabled(true)
                    .supportedLocales("en", "de");
            return realm;
        }
    }
}
