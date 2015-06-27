package org.keycloak.testsuite.federation;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPMultipleAttributesTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static UserFederationProviderModel ldapModel = null;

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            Map<String,String> ldapConfig = ldapRule.getConfig();
            ldapConfig.put(LDAPConstants.EDIT_MODE, UserFederationProvider.EditMode.WRITABLE.toString());

            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "test-ldap", -1, -1, 0);
            FederationTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Test
    public void testModel() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            FederationTestUtils.assertUserImported(session.users(), appRealm, "jbrown", "James", "Brown", "jbrown@keycloak.org", "88441");

            UserModel user = session.users().getUserByUsername("bwilson", appRealm);
            Assert.assertEquals("bwilson@keycloak.org", user.getEmail());
            Assert.assertEquals("Bruce", user.getFirstName());

            // There are 2 lastnames in ldif
            Assert.assertTrue("Wilson".equals(user.getLastName()) || "Schneider".equals(user.getLastName()));

            // Actually there are 2 postalCodes
            List<String> postalCodes = user.getAttribute("postal_code");
            assertPostalCodes(postalCodes, "88441", "77332");

            postalCodes.remove("77332");
            user.setAttribute("postal_code", postalCodes);

        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("bwilson", appRealm);
            List<String> postalCodes = user.getAttribute("postal_code");
            assertPostalCodes(postalCodes, "88441");

            postalCodes.add("77332");
            user.setAttribute("postal_code", postalCodes);
            assertPostalCodes(user.getAttribute("postal_code"), "88441", "77332");
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    private void assertPostalCodes(List<String> postalCodes, String... expectedPostalCodes) {
        if (expectedPostalCodes == null && postalCodes.isEmpty()) {
            return;
        }


        Assert.assertEquals(expectedPostalCodes.length, postalCodes.size());
        for (String expected : expectedPostalCodes) {
            if (!postalCodes.contains(expected)) {
                Assert.fail("postalCode '" + expected + "' not in postalCodes: " + postalCodes);
            }
        }
    }



}


