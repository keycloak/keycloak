package org.keycloak.testsuite.federation;

import org.junit.Assert;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.representations.idm.CredentialRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class FederationTestUtils {

    public static UserModel addLocalUser(KeycloakSession session, RealmModel realm, String username, String email, String password) {
        UserModel user = session.users().addUser(realm, username);
        user.setEmail(email);
        user.setEnabled(true);

        UserCredentialModel creds = new UserCredentialModel();
        creds.setType(CredentialRepresentation.PASSWORD);
        creds.setValue(password);

        user.updateCredential(creds);
        return user;
    }

    public static LDAPObject addLDAPUser(LDAPFederationProvider ldapProvider, RealmModel realm, final String username,
                                            final String firstName, final String lastName, final String email, final String postalCode) {
        UserModel helperUser = new UserModelDelegate(null) {

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getFirstName() {
                return firstName;
            }

            @Override
            public String getLastName() {
                return lastName;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getAttribute(String name) {
                if (name == "postal_code") {
                    return postalCode;
                } else {
                    return null;
                }
            }
        };
        return LDAPUtils.addUserToLDAP(ldapProvider, realm, helperUser);
    }

    public static LDAPFederationProvider getLdapProvider(KeycloakSession keycloakSession, UserFederationProviderModel ldapFedModel) {
        LDAPFederationProviderFactory ldapProviderFactory = (LDAPFederationProviderFactory) keycloakSession.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, ldapFedModel.getProviderName());
        return ldapProviderFactory.getInstance(keycloakSession, ldapFedModel);
    }

    public static void assertUserImported(UserProvider userProvider, RealmModel realm, String username, String expectedFirstName, String expectedLastName, String expectedEmail, String expectedPostalCode) {
        UserModel user = userProvider.getUserByUsername(username, realm);
        Assert.assertNotNull(user);
        Assert.assertEquals(expectedFirstName, user.getFirstName());
        Assert.assertEquals(expectedLastName, user.getLastName());
        Assert.assertEquals(expectedEmail, user.getEmail());
        Assert.assertEquals(expectedPostalCode, user.getAttribute("postal_code"));
    }
}
