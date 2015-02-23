package org.keycloak.testsuite.federation;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosConfig;
import org.keycloak.federation.kerberos.KerberosFederationProviderFactory;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.models.KerberosConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KerberosRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * Test of LDAPFederationProvider (Kerberos backed by LDAP)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosLdapTest extends AbstractKerberosTest {

    public static final String CONFIG_LOCATION = "kerberos/kerberos-ldap-connection.properties";

    private static UserFederationProviderModel ldapModel = null;

    private static KerberosRule kerberosRule = new KerberosRule(CONFIG_LOCATION);

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            Map<String,String> ldapConfig = kerberosRule.getConfig();
            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "kerberos-ldap", -1, -1, 0);
            appRealm.addRequiredCredential(UserCredentialModel.KERBEROS);
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(kerberosRule)
            .around(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new LDAPProviderKerberosConfig(ldapModel);
    }

    @Override
    protected KeycloakRule getKeycloakRule() {
        return keycloakRule;
    }

    @Override
    protected AssertEvents getAssertEvents() {
        return events;
    }


    @Test
    public void spnegoLoginTest() throws Exception {
        spnegoLoginTestImpl();

        // Assert user was imported and hasn't any required action on him. Profile info is synced from LDAP
        assertUser("hnelson", "hnelson@keycloak.org", "Horatio", "Nelson", false);
    }


    @Test
    public void writableEditModeTest() throws Exception {
        KeycloakRule keycloakRule = getKeycloakRule();
        AssertEvents events = getAssertEvents();

        // Change editMode to READ_ONLY
        updateProviderEditMode(UserFederationProvider.EditMode.WRITABLE);

        // Login with username/password from kerberos
        changePasswordPage.open();
        loginPage.assertCurrent();
        loginPage.login("jduke", "theduke");
        changePasswordPage.assertCurrent();

        // Successfully change password now
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated"));
        changePasswordPage.logout();

        // Login with old password doesn't work, but with new password works
        loginPage.login("jduke", "theduke");
        loginPage.assertCurrent();
        loginPage.login("jduke", "newPass");
        changePasswordPage.assertCurrent();
        changePasswordPage.logout();

        // Assert SPNEGO login with the new password as mode is writable
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "newPass");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        events.expectLogin()
                .user(keycloakRule.getUser("test", "jduke").getId())
                .detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "jduke")
                .assertEvent();

        // Change password back
        loginPage.login("jduke", "newPass");
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("newPass", "theduke", "theduke");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated"));
        changePasswordPage.logout();

        spnegoResponse.close();
        events.clear();
    }

}
