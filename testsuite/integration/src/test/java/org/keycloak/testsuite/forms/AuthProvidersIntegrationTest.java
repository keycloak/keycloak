package org.keycloak.testsuite.forms;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.model.test.LdapTestUtils;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.authentication.AuthProviderConstants;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuthProvidersIntegrationTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            addUser(appRealm, "mary", "mary@test.com", "password-app");
            addUser(adminstrationRealm, "mary-admin", "mary@admin.com", "password-admin");

            AuthenticationProviderModel modelProvider = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_MODEL, false, Collections.EMPTY_MAP);
            AuthenticationProviderModel picketlinkProvider = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_PICKETLINK, true, Collections.EMPTY_MAP);

            // Delegate authentication to admin realm
            Map<String,String> config = new HashMap<String,String>();
            config.put(AuthProviderConstants.EXTERNAL_REALM_ID, adminstrationRealm.getId());
            AuthenticationProviderModel externalModelProvider = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_EXTERNAL_MODEL, true, config);

            appRealm.setAuthenticationProviders(Arrays.asList(modelProvider, picketlinkProvider, externalModelProvider));

            // Configure LDAP
            ldapRule.getEmbeddedServer().setupLdapInRealm(appRealm);
            LdapTestUtils.setLdapPassword(appRealm, "john", "password");
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected RegisterPage registerPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected AccountUpdateProfilePage profilePage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;

    private static UserModel addUser(RealmModel realm, String username, String email, String password) {
        UserModel user = realm.addUser(username);
        user.setEmail(email);
        user.setEnabled(true);

        UserCredentialModel creds = new UserCredentialModel();
        creds.setType(CredentialRepresentation.PASSWORD);
        creds.setValue(password);

        realm.updateCredential(user, creds);
        return user;
    }

    @Test
    public void loginClassic() {
        loginPage.open();
        loginPage.login("mary", "password-app");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        profilePage.open();
        Assert.assertFalse(profilePage.isPasswordUpdateSupported());
    }

    @Test
    public void loginExternalModel() {
        loginPage.open();
        loginPage.login("mary-admin", "password-admin");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        profilePage.open();
        Assert.assertTrue(profilePage.isPasswordUpdateSupported());
    }

    @Test
    public void loginLdap() {
        loginPage.open();
        loginPage.login("john", "password");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        profilePage.open();
        Assert.assertTrue(profilePage.isPasswordUpdateSupported());
        Assert.assertEquals("John", profilePage.getFirstName());
        Assert.assertEquals("Doe", profilePage.getLastName());
        Assert.assertEquals("john@email.org", profilePage.getEmail());
    }

    @Test
    public void passwordChangeExternalModel() {
        // Set password-policy for admin realm
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                adminstrationRealm.setPasswordPolicy(new PasswordPolicy("length(6)"));
            }
        });

        try {
            changePasswordPage.open();
            loginPage.login("mary-admin", "password-admin");

            // Can't update to "pass" due to passwordPolicy
            changePasswordPage.changePassword("password-admin", "pass", "pass");
            Assert.assertEquals("Invalid password: minimum length 6", profilePage.getError());

            changePasswordPage.changePassword("password-admin", "password-updated", "password-updated");
            Assert.assertEquals("Your password has been updated", profilePage.getSuccess());
            changePasswordPage.logout();

            loginPage.open();
            loginPage.login("mary-admin", "password-updated");
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        } finally {
            keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    adminstrationRealm.setPasswordPolicy(new PasswordPolicy(null));
                }
            });
        }
    }

    @Test
    public void passwordChangeLdap() throws Exception {
        changePasswordPage.open();
        loginPage.login("john", "password");
        changePasswordPage.changePassword("password", "new-password", "new-password");

        Assert.assertEquals("Your password has been updated", profilePage.getSuccess());

        changePasswordPage.logout();

//        TODO: Disabled until https://issues.jboss.org/browse/PLINK-384 is released and updated
//        loginPage.open();
//        loginPage.login("john", "password");
//        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        loginPage.open();
        loginPage.login("john", "new-password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }

    @Test
    public void registerExistingLdapUser() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "email", "existing", "password", "password");

        registerPage.assertCurrent();
        Assert.assertEquals("Username already exists", registerPage.getError());
    }

    @Test
    public void registerUserLdapSuccess() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "email", "registerUserSuccess", "password", "password");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
    }
}
