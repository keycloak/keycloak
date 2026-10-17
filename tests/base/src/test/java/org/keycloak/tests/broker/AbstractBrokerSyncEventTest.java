package org.keycloak.tests.broker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.provider.BrokeredUserChangeTracker;
import org.keycloak.broker.provider.HardcodedAttributeMapper;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.IdentityProviderMapperBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Protocol-agnostic tests verifying that the changes made to an existing user while it is synchronized from an
 * identity provider are reported like other profile updates, i.e. a changed email in an {@link EventType#UPDATE_EMAIL}
 * event and other changes in a single {@link EventType#UPDATE_PROFILE} event, and that no event is sent if the stored
 * values do not change. Concrete tests provide the provider and consumer realms of the respective protocol, where the
 * identity provider uses the sync mode {@code FORCE}, and the provider sends the {@value #DEPARTMENT} attribute that the
 * consumer maps to a user attribute using an identity provider mapper.
 */
public abstract class AbstractBrokerSyncEventTest {

    protected static final String PROVIDER_REALM = "provider";
    protected static final String CONSUMER_REALM = "consumer";
    protected static final String SERVER_ROOT = "http://localhost:8080";
    protected static final String USER_LOGIN = "testuser";
    protected static final String USER_PASSWORD = "password";
    protected static final String USER_EMAIL = USER_LOGIN + "@example.com";
    protected static final String DEPARTMENT = "department";
    private static final String CHANGED_EMAIL = "changed@example.com";

    @InjectOAuthClient(realmRef = CONSUMER_REALM)
    protected OAuthClient oauth;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectPage
    protected LoginPage loginPage;

    @InjectEvents(realmRef = CONSUMER_REALM)
    protected Events events;

    protected abstract ManagedRealm getProviderRealm();

    protected abstract ManagedRealm getConsumerRealm();

    protected abstract String getIdpAlias();

    @BeforeEach
    void setUpProviderUser() {
        UserProfileUtil.enableUnmanagedAttributes(getProviderRealm().admin().users().userProfile());
        UserProfileUtil.enableUnmanagedAttributes(getConsumerRealm().admin().users().userProfile());
        updateProviderUser(user -> user.singleAttribute(DEPARTMENT, "sales"));
    }

    @Test
    public void testChangedAttributesReportedInSingleEvent() {
        loginThroughBrokerAndLogout();
        events.skipAll();

        updateProviderUser(user -> {
            user.setFirstName("Changed");
            user.singleAttribute(DEPARTMENT, "engineering");
        });
        loginThroughBrokerAndLogout();

        List<EventRepresentation> syncEvents = pollSyncEvents();
        Assertions.assertEquals(1, syncEvents.size());
        assertSyncEvent(syncEvents.get(0), EventType.UPDATE_PROFILE)
                .details(Details.PREVIOUS_FIRST_NAME, "Test")
                .details(Details.UPDATED_FIRST_NAME, "Changed")
                .details(Details.PREF_PREVIOUS + DEPARTMENT, "sales")
                .details(Details.PREF_UPDATED + DEPARTMENT, "engineering");

        UserRepresentation consumerUser = getConsumerUser();
        Assertions.assertEquals("Changed", consumerUser.getFirstName());
        Assertions.assertEquals(List.of("engineering"), consumerUser.getAttributes().get(DEPARTMENT));
    }

    @Test
    public void testRemovedAttributeReported() {
        loginThroughBrokerAndLogout();
        events.skipAll();

        updateProviderUser(user -> user.getAttributes().remove(DEPARTMENT));
        loginThroughBrokerAndLogout();

        List<EventRepresentation> syncEvents = pollSyncEvents();
        Assertions.assertEquals(1, syncEvents.size());
        assertSyncEvent(syncEvents.get(0), EventType.UPDATE_PROFILE)
                .details(Details.PREF_PREVIOUS + DEPARTMENT, "sales")
                .withoutDetails(Details.PREF_UPDATED + DEPARTMENT);

        UserRepresentation consumerUser = getConsumerUser();
        Assertions.assertTrue(consumerUser.getAttributes() == null || !consumerUser.getAttributes().containsKey(DEPARTMENT));
    }

    @Test
    public void testChangedEmailReportedAsUpdateEmail() {
        loginThroughBrokerAndLogout();
        events.skipAll();

        updateProviderUser(user -> user.setEmail(CHANGED_EMAIL));
        loginThroughBrokerAndLogout();

        List<EventRepresentation> syncEvents = pollSyncEvents();
        Assertions.assertEquals(1, syncEvents.size());
        assertSyncEvent(syncEvents.get(0), EventType.UPDATE_EMAIL)
                .details(Details.PREVIOUS_EMAIL, USER_EMAIL)
                .details(Details.UPDATED_EMAIL, CHANGED_EMAIL);

        Assertions.assertEquals(CHANGED_EMAIL, getConsumerUser().getEmail());
    }

    @Test
    public void testChangedEmailAndOtherAttributesReportedInSeparateEvents() {
        loginThroughBrokerAndLogout();
        events.skipAll();

        updateProviderUser(user -> {
            user.setEmail(CHANGED_EMAIL);
            user.singleAttribute(DEPARTMENT, "engineering");
        });
        loginThroughBrokerAndLogout();

        List<EventRepresentation> syncEvents = pollSyncEvents();
        Assertions.assertEquals(2, syncEvents.size());
        assertSyncEvent(findEvent(syncEvents, EventType.UPDATE_EMAIL), EventType.UPDATE_EMAIL)
                .details(Details.PREVIOUS_EMAIL, USER_EMAIL)
                .details(Details.UPDATED_EMAIL, CHANGED_EMAIL)
                .withoutDetails(Details.PREF_PREVIOUS + DEPARTMENT, Details.PREF_UPDATED + DEPARTMENT);
        assertSyncEvent(findEvent(syncEvents, EventType.UPDATE_PROFILE), EventType.UPDATE_PROFILE)
                .details(Details.PREF_PREVIOUS + DEPARTMENT, "sales")
                .details(Details.PREF_UPDATED + DEPARTMENT, "engineering")
                .withoutDetails(Details.PREVIOUS_EMAIL, Details.UPDATED_EMAIL);
    }

    @Test
    public void testChangedUsernameReported() {
        loginThroughBrokerAndLogout();
        String userId = getConsumerUser().getId();
        addIdpMapper(createIdpMapper(getIdpAlias(), "renamed-username", HardcodedAttributeMapper.PROVIDER_ID)
                .attribute(HardcodedAttributeMapper.ATTRIBUTE, UserModel.USERNAME)
                .attribute(HardcodedAttributeMapper.ATTRIBUTE_VALUE, "renamed"));
        events.skipAll();

        // the user cannot be found by its previous username to log out afterwards
        loginThroughBroker();

        List<EventRepresentation> syncEvents = pollSyncEvents();
        Assertions.assertEquals(1, syncEvents.size());
        assertSyncEvent(syncEvents.get(0), EventType.UPDATE_PROFILE, userId, "renamed")
                .details(Details.PREF_PREVIOUS + UserModel.USERNAME, USER_LOGIN)
                .details(Details.PREF_UPDATED + UserModel.USERNAME, "renamed");
    }

    @Test
    public void testLegacySyncModeReported() {
        loginThroughBrokerAndLogout();
        updateIdentityProvider(idp -> idp.getConfig().put(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.LEGACY.name()));
        events.skipAll();

        updateProviderUser(user -> user.singleAttribute(DEPARTMENT, "engineering"));
        loginThroughBrokerAndLogout();

        List<EventRepresentation> syncEvents = pollSyncEvents();
        Assertions.assertEquals(1, syncEvents.size());
        assertSyncEvent(syncEvents.get(0), EventType.UPDATE_PROFILE)
                .details(Details.PREF_PREVIOUS + DEPARTMENT, "sales")
                .details(Details.PREF_UPDATED + DEPARTMENT, "engineering");
    }

    @Test
    public void testNoEventIfStoredValuesUnchanged() {
        loginThroughBrokerAndLogout();
        addMixedCaseEmailMapper();
        events.skipAll();

        loginThroughBrokerAndLogout();

        Assertions.assertEquals(List.of(), pollSyncEvents());
        Assertions.assertEquals(USER_EMAIL, getConsumerUser().getEmail());
    }

    @Test
    public void testNormalizedEmailNotReportedWithOtherChanges() {
        loginThroughBrokerAndLogout();
        addMixedCaseEmailMapper();
        events.skipAll();

        // the first name is changed first, so the email is written to the database instead of being skipped by the cache
        updateProviderUser(user -> user.setFirstName("Changed"));
        loginThroughBrokerAndLogout();

        List<EventRepresentation> syncEvents = pollSyncEvents();
        Assertions.assertEquals(1, syncEvents.size());
        assertSyncEvent(syncEvents.get(0), EventType.UPDATE_PROFILE)
                .details(Details.PREVIOUS_FIRST_NAME, "Test")
                .details(Details.UPDATED_FIRST_NAME, "Changed")
                .withoutDetails(Details.PREVIOUS_EMAIL, Details.UPDATED_EMAIL);
        Assertions.assertEquals(USER_EMAIL, getConsumerUser().getEmail());
    }

    protected static UserBuilder createProviderUser() {
        // The provider user is complete (first/last name, verified email) so the consumer's
        // first-broker-login review-profile page does not interrupt the redirect back.
        return UserBuilder.create(USER_LOGIN)
                .password(USER_PASSWORD)
                .email(USER_EMAIL)
                .emailVerified(true)
                .firstName("Test")
                .lastName("User")
                .enabled(true);
    }

    protected static IdentityProviderMapperBuilder createIdpMapper(String idpAlias, String name, String mapperType) {
        return IdentityProviderMapperBuilder.create()
                .name(name)
                .identityProviderAlias(idpAlias)
                .identityProviderMapper(mapperType)
                .attribute(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.name());
    }

    // sets the email in a different case on each login, which is stored lower-cased
    private void addMixedCaseEmailMapper() {
        addIdpMapper(createIdpMapper(getIdpAlias(), "mixed-case-email", HardcodedAttributeMapper.PROVIDER_ID)
                .attribute(HardcodedAttributeMapper.ATTRIBUTE, UserModel.EMAIL)
                .attribute(HardcodedAttributeMapper.ATTRIBUTE_VALUE, "TestUser@Example.com"));
    }

    private void addIdpMapper(IdentityProviderMapperBuilder mapper) {
        IdentityProviderMapperRepresentation representation = mapper.build();
        getConsumerRealm().admin().identityProviders().get(getIdpAlias()).addMapper(representation).close();
    }

    private void updateIdentityProvider(Consumer<IdentityProviderRepresentation> update) {
        IdentityProviderResource resource = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation representation = resource.toRepresentation();
        update.accept(representation);
        resource.update(representation);
    }

    private void loginThroughBrokerAndLogout() {
        loginThroughBroker();
        AccountHelper.logout(getConsumerRealm().admin(), USER_LOGIN);
        AccountHelper.logout(getProviderRealm().admin(), USER_LOGIN);
    }

    private void loginThroughBroker() {
        oauth.openLoginForm();
        loginPage.clickSocial(getIdpAlias());
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    private List<EventRepresentation> pollSyncEvents() {
        List<EventRepresentation> syncEvents = new ArrayList<>();
        for (EventRepresentation event = events.poll(); event != null; event = events.poll()) {
            if (EventType.UPDATE_PROFILE.name().equals(event.getType()) || EventType.UPDATE_EMAIL.name().equals(event.getType())) {
                syncEvents.add(event);
            }
        }
        return syncEvents;
    }

    private static EventRepresentation findEvent(List<EventRepresentation> events, EventType type) {
        return events.stream().filter(event -> type.name().equals(event.getType())).findFirst().orElseThrow();
    }

    private EventAssertion assertSyncEvent(EventRepresentation event, EventType type) {
        return assertSyncEvent(event, type, getConsumerUser().getId(), USER_LOGIN);
    }

    private EventAssertion assertSyncEvent(EventRepresentation event, EventType type, String userId, String username) {
        return EventAssertion.assertSuccess(event)
                .type(type)
                .userId(userId)
                .details(Details.CONTEXT, BrokeredUserChangeTracker.IDP_SYNC_CONTEXT)
                .details(Details.USERNAME, username)
                .details(Details.IDENTITY_PROVIDER, getIdpAlias())
                .details(Details.IDENTITY_PROVIDER_USERNAME, USER_LOGIN);
    }

    private void updateProviderUser(Consumer<UserRepresentation> update) {
        UserRepresentation user = getProviderRealm().admin().users().search(USER_LOGIN, true).get(0);
        UserResource userResource = getProviderRealm().admin().users().get(user.getId());
        user = userResource.toRepresentation();
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<>());
        }
        update.accept(user);
        userResource.update(user);
    }

    private UserRepresentation getConsumerUser() {
        List<UserRepresentation> users = getConsumerRealm().admin().users().search(USER_LOGIN, true);
        Assertions.assertEquals(1, users.size());
        return getConsumerRealm().admin().users().get(users.get(0).getId()).toRepresentation();
    }
}
