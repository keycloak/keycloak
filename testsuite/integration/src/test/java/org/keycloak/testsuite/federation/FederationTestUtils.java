package org.keycloak.testsuite.federation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.mappers.RoleLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.RoleLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapperFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.representations.idm.CredentialRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class FederationTestUtils {

    public static UserModel addLocalUser(KeycloakSession session, RealmModel realm, String username, String email, String password) {
        UserModel user = session.userStorage().addUser(realm, username);
        user.setEmail(email);
        user.setEnabled(true);

        UserCredentialModel creds = new UserCredentialModel();
        creds.setType(CredentialRepresentation.PASSWORD);
        creds.setValue(password);

        user.updateCredential(creds);
        return user;
    }

    public static LDAPObject addLDAPUser(LDAPFederationProvider ldapProvider, RealmModel realm, final String username,
                                            final String firstName, final String lastName, final String email, final String street, final String... postalCode) {
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
            public List<String> getAttribute(String name) {
                if ("postal_code".equals(name) && postalCode != null && postalCode.length > 0) {
                    return Arrays.asList(postalCode);
                } else if ("street".equals(name) && street != null) {
                    return Arrays.asList(street);
                } else {
                    return Collections.emptyList();
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
        Assert.assertEquals(expectedPostalCode, user.getFirstAttribute("postal_code"));
    }

    public static void addZipCodeLDAPMapper(RealmModel realm, UserFederationProviderModel providerModel) {
        addUserAttributeMapper(realm, providerModel, "zipCodeMapper", "postal_code", LDAPConstants.POSTAL_CODE);
    }

    public static UserFederationMapperModel addUserAttributeMapper(RealmModel realm, UserFederationProviderModel providerModel, String mapperName, String userModelAttributeName, String ldapAttributeName) {
        UserFederationMapperModel mapperModel = KeycloakModelUtils.createUserFederationMapperModel(mapperName, providerModel.getId(), UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, userModelAttributeName,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, ldapAttributeName,
                UserAttributeLDAPFederationMapper.READ_ONLY, "false",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "false");
        return realm.addUserFederationMapper(mapperModel);
    }

    public static void addOrUpdateRoleLDAPMappers(RealmModel realm, UserFederationProviderModel providerModel, RoleLDAPFederationMapper.Mode mode) {
        UserFederationMapperModel mapperModel = realm.getUserFederationMapperByName(providerModel.getId(), "realmRolesMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().put(RoleLDAPFederationMapper.MODE, mode.toString());
            realm.updateUserFederationMapper(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().get(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createUserFederationMapperModel("realmRolesMapper", providerModel.getId(), RoleLDAPFederationMapperFactory.PROVIDER_ID,
                    RoleLDAPFederationMapper.ROLES_DN, "ou=RealmRoles," + baseDn,
                    RoleLDAPFederationMapper.USE_REALM_ROLES_MAPPING, "true",
                    RoleLDAPFederationMapper.MODE, mode.toString());
            realm.addUserFederationMapper(mapperModel);
        }

        mapperModel = realm.getUserFederationMapperByName(providerModel.getId(), "financeRolesMapper");
        if (mapperModel != null) {
            mapperModel.getConfig().put(RoleLDAPFederationMapper.MODE, mode.toString());
            realm.updateUserFederationMapper(mapperModel);
        } else {
            String baseDn = providerModel.getConfig().get(LDAPConstants.BASE_DN);
            mapperModel = KeycloakModelUtils.createUserFederationMapperModel("financeRolesMapper", providerModel.getId(), RoleLDAPFederationMapperFactory.PROVIDER_ID,
                    RoleLDAPFederationMapper.ROLES_DN, "ou=FinanceRoles," + baseDn,
                    RoleLDAPFederationMapper.USE_REALM_ROLES_MAPPING, "false",
                    RoleLDAPFederationMapper.CLIENT_ID, "finance",
                    RoleLDAPFederationMapper.MODE, mode.toString());
            realm.addUserFederationMapper(mapperModel);
        }
    }

    public static void removeAllLDAPUsers(LDAPFederationProvider ldapProvider, RealmModel realm) {
        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(ldapProvider, realm);
        List<LDAPObject> allUsers = ldapQuery.getResultList();

        for (LDAPObject ldapUser : allUsers) {
            ldapStore.remove(ldapUser);
        }
    }

    public static void removeAllLDAPRoles(KeycloakSession session, RealmModel appRealm, UserFederationProviderModel ldapModel, String mapperName) {
        UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), mapperName);
        LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
        LDAPQuery roleQuery = new RoleLDAPFederationMapper().createRoleQuery(mapperModel, ldapProvider);
        List<LDAPObject> ldapRoles = roleQuery.getResultList();
        for (LDAPObject ldapRole : ldapRoles) {
            ldapProvider.getLdapIdentityStore().remove(ldapRole);
        }
    }

    public static void createLDAPRole(KeycloakSession session, RealmModel appRealm, UserFederationProviderModel ldapModel, String mapperName, String roleName) {
        UserFederationMapperModel mapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), mapperName);
        LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
        new RoleLDAPFederationMapper().createLDAPRole(mapperModel, roleName, ldapProvider);
    }
}
