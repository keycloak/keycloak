package org.keycloak.testsuite.federation.ldap;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.RequiredActionLDAPGroupStorageMapper;
import org.keycloak.storage.ldap.mappers.RequiredActionLDAPGroupStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.MembershipType;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.Collections;
import java.util.HashMap;

/**
 * Integration tests for {@link RequiredActionLDAPGroupStorageMapper}.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPRequiredActionGroupStorageMapperTest extends AbstractLDAPTest {

    private static final String GROUPS_DN = "ou=Groups,dc=keycloak,dc=org";
    private static final String REQUIRED_ACTION_GROUP_NAME = "required-action-group";
    private static final String MAPPER_NAME = "requiredActionGroupMapper";

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    // -----------------------------------------------------------------------
    // Test realm / LDAP setup
    // -----------------------------------------------------------------------

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel mapperModel = KeycloakModelUtils.createComponentModel(
                    MAPPER_NAME,
                    ctx.getLdapModel().getId(),
                    RequiredActionLDAPGroupStorageMapperFactory.PROVIDER_ID,
                    LDAPStorageMapper.class.getName(),
                    RequiredActionLDAPGroupStorageMapperFactory.GROUP,              REQUIRED_ACTION_GROUP_NAME,
                    RequiredActionLDAPGroupStorageMapperFactory.GROUPS_DN,          GROUPS_DN,
                    RequiredActionLDAPGroupStorageMapperFactory.MEMBERSHIP_ATTR_NAME, LDAPConstants.MEMBER
            );
            mapperModel.getConfig().add(
                    RequiredActionLDAPGroupStorageMapperFactory.REQUIRED_ACTION,
                    UserModel.RequiredAction.VERIFY_EMAIL.name()
            );
            appRealm.addComponentModel(mapperModel);

            // Provision LDAP users
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPTestUtils.removeAllLDAPUsers(ldapProvider, appRealm);

            // John will be placed in the group → should receive the required action.
            LDAPObject john = LDAPTestUtils.addLDAPUser(
                    ldapProvider, appRealm,
                    "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapProvider, john, "Password1");

            // Mary will NOT be in the group → must NOT receive the required action.
            LDAPObject mary = LDAPTestUtils.addLDAPUser(
                    ldapProvider, appRealm,
                    "marykeycloak", "Mary", "Kelly", "mary@email.org", null, "5678");
            LDAPTestUtils.updateLDAPPassword(ldapProvider, mary, "Password1");

            // Create LDAP group and add John
            LDAPObject ldapGroup = LDAPUtils.createLDAPGroup(
                    ldapProvider,
                    REQUIRED_ACTION_GROUP_NAME,
                    "cn",
                    Collections.singletonList("groupOfNames"),
                    GROUPS_DN,
                    new HashMap<>(),
                    LDAPConstants.MEMBER
            );
            LDAPUtils.addMember(ldapProvider, MembershipType.DN,
                    LDAPConstants.MEMBER, "not-used", ldapGroup, john);
        });
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    public void test_01_userInGroupGetsRequiredAction() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Loading the user triggers a forced import from LDAP (onImportUserFromLDAP).
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertNotNull("johnkeycloak must be importable from LDAP", john);

            Assert.assertTrue(
                    "John (member of the LDAP group) must have the VERIFY_EMAIL required action",
                    john.getRequiredActionsStream()
                            .anyMatch(a -> UserModel.RequiredAction.VERIFY_EMAIL.name().equals(a))
            );

            // The tracking attribute must be set so the mapper knows it already acted.
            String trackerAttr = RequiredActionLDAPGroupStorageMapper
                    .USER_ATTR_FOR_REMEMBERING_REQUIRED_ACTIONS_PREFIX + MAPPER_NAME;
            Assert.assertEquals(
                    "Tracker attribute must equal the joined required-action list",
                    UserModel.RequiredAction.VERIFY_EMAIL.name(),
                    john.getFirstAttribute(trackerAttr)
            );
        });
    }


    @Test
    public void test_02_userNotInGroupDoesNotGetRequiredAction() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel mary = session.users().getUserByUsername(appRealm, "marykeycloak");
            Assert.assertNotNull("marykeycloak must be importable from LDAP", mary);

            Assert.assertFalse(
                    "Mary (not a member of the LDAP group) must NOT have the VERIFY_EMAIL required action",
                    mary.getRequiredActionsStream()
                            .anyMatch(a -> UserModel.RequiredAction.VERIFY_EMAIL.name().equals(a))
            );
        });
    }


    @Test
    public void test_03_requiredActionNotReAddedAfterCompletion() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertNotNull(john);

            // Simulate the user completing the required action.
            john.removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.name());

            // Sanity-check: the tracker attribute must still be present.
            String trackerAttr = RequiredActionLDAPGroupStorageMapper
                    .USER_ATTR_FOR_REMEMBERING_REQUIRED_ACTIONS_PREFIX + MAPPER_NAME;
            Assert.assertNotNull(
                    "Tracker attribute must still be present after the user completed the action",
                    john.getFirstAttribute(trackerAttr)
            );
        });

        //trigger another import and verify VERIFY_EMAIL is NOT re-added.
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // getUserByUsername always triggers a forced import (ImportType.FORCED),
            // which calls onImportUserFromLDAP on every registered mapper.
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            Assert.assertNotNull(john);

            Assert.assertFalse(
                    "VERIFY_EMAIL must NOT be re-added once the user has already completed it",
                    john.getRequiredActionsStream()
                            .anyMatch(a -> UserModel.RequiredAction.VERIFY_EMAIL.name().equals(a))
            );
        });
    }
}
