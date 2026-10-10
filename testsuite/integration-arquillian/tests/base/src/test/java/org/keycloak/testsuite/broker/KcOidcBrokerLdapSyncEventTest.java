package org.keycloak.testsuite.broker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.provider.BrokeredUserChangeTracker;
import org.keycloak.broker.provider.HardcodedAttributeMapper;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.storage.UserStorageProvider.EditMode;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.ldap.LDAPTestContext;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentationWithoutConfig;

/**
 * Verifies that only the changes made by the identity provider sync are reported for a user federated from LDAP,
 * even if the cached user is stale because the user was changed directly in LDAP.
 */
public final class KcOidcBrokerLdapSyncEventTest extends AbstractInitializedBaseBrokerTest {

    private static final String DEPARTMENT = "department";
    private static final String LDAP_PASSWORD = "Password1";

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    private IdpConfirmLinkPage confirmLinkPage;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
                return super.setUpIdentityProvider(IdentityProviderSyncMode.FORCE);
            }
        };
    }

    @Before
    public void onBefore() {
        createLdapStorageProvider();
        addLdapUser(bc.getUserLogin(), bc.getUserEmail());
    }

    @Test
    public void testChangeInLdapNotReportedAsIdentityProviderChange() {
        String departmentMapperId = addDepartmentMapper("sales");

        // federate the user and link it to the existing LDAP user
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        logInWithBroker(bc);
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "f", "l");
        confirmLinkPage.clickLinkAccount();
        loginPage.login(bc.getUserLogin(), LDAP_PASSWORD);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        // the existing user is updated when linked to the identity provider
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        String userId = consumerRealm.users().search(bc.getUserLogin()).get(0).getId();
        List<EventRepresentation> linkEvents = BrokerTestTools.pollProfileUpdateEvents(events, userId);
        Assertions.assertEquals(1, linkEvents.size());
        EventAssertion.assertSuccess(linkEvents.get(0))
                .type(EventType.UPDATE_PROFILE)
                .details(Details.CONTEXT, BrokeredUserChangeTracker.IDP_SYNC_CONTEXT)
                .details(Details.PREF_UPDATED + DEPARTMENT, "sales")
                .withoutDetails(Details.PREF_PREVIOUS + DEPARTMENT);

        // the admin logout sets the not-before time of the user, which evicts it from the cache, so the user is
        // read afterwards to cache it again before it is changed in LDAP
        consumerRealm.users().get(userId).logout();
        Assertions.assertEquals("l", consumerRealm.users().get(userId).toRepresentation().getLastName());

        // change the last name directly in LDAP, the cached user still has the previous one
        String realmName = bc.consumerRealmName();
        String username = bc.getUserLogin();
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, realmName, null);
            LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(ctx.getRealm(), username);
            ldapUser.setSingleAttribute(LDAPConstants.SN, "changed-in-ldap");
            ctx.getLdapProvider().getLdapIdentityStore().update(ldapUser);
        });
        Assertions.assertEquals("l", consumerRealm.users().get(userId).toRepresentation().getLastName());

        // the identity provider sync only changes the department on the next login
        updateDepartmentMapper(departmentMapperId, "engineering");
        events.clear();
        oauth.openLoginForm();
        logInWithBroker(bc);
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        List<EventRepresentation> syncEvents = BrokerTestTools.pollProfileUpdateEvents(events, userId);
        Assertions.assertEquals(1, syncEvents.size());
        EventRepresentation syncEvent = EventAssertion.assertSuccess(syncEvents.get(0))
                .type(EventType.UPDATE_PROFILE)
                .details(Details.CONTEXT, BrokeredUserChangeTracker.IDP_SYNC_CONTEXT)
                .details(Details.PREF_PREVIOUS + DEPARTMENT, "sales")
                .details(Details.PREF_UPDATED + DEPARTMENT, "engineering")
                .getEvent();
        // neither the last name nor the modification timestamp changed in LDAP are reported
        Assertions.assertEquals(Set.of(Details.PREF_PREVIOUS + DEPARTMENT, Details.PREF_UPDATED + DEPARTMENT), syncEvent.getDetails().keySet().stream()
                .filter(key -> key.startsWith(Details.PREF_PREVIOUS) || key.startsWith(Details.PREF_UPDATED))
                .collect(Collectors.toSet()));
    }

    private String addDepartmentMapper(String department) {
        try (Response response = identityProviderResource.addMapper(createDepartmentMapper(department))) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private void updateDepartmentMapper(String id, String department) {
        IdentityProviderMapperRepresentation mapper = createDepartmentMapper(department);
        mapper.setId(id);
        identityProviderResource.update(id, mapper);
    }

    private IdentityProviderMapperRepresentation createDepartmentMapper(String department) {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName(DEPARTMENT);
        mapper.setIdentityProviderAlias(bc.getIDPAlias());
        mapper.setIdentityProviderMapper(HardcodedAttributeMapper.PROVIDER_ID);
        mapper.setConfig(Map.of(
                HardcodedAttributeMapper.ATTRIBUTE, DEPARTMENT,
                HardcodedAttributeMapper.ATTRIBUTE_VALUE, department,
                IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.name()));
        return mapper;
    }

    private void createLdapStorageProvider() {
        Map<String, String> ldapConfig = ldapRule.getConfig();
        ldapConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "false");
        ldapConfig.put(LDAPConstants.EDIT_MODE, EditMode.WRITABLE.name());
        ldapConfig.put(UserStorageProviderModel.IMPORT_ENABLED, "true");
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        ldapConfig.forEach(config::add);

        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setLastSync(0);
        model.setChangedSyncPeriod(-1);
        model.setFullSyncPeriod(-1);
        model.setName("ldap");
        model.setPriority(0);
        model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
        model.setConfig(config);

        try (Response response = adminClient.realm(bc.consumerRealmName()).components().add(toRepresentationWithoutConfig(model))) {
            getCleanup().addComponentId(ApiUtil.getCreatedId(response));
        }
    }

    private void addLdapUser(String username, String email) {
        String realmName = bc.consumerRealmName();

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session, realmName, null);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);
            LDAPObject user = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, username, "f", "l", email, new MultivaluedHashMap<>());
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), user, LDAP_PASSWORD);
        });
    }
}
