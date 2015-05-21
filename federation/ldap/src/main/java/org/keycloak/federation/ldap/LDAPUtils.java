package org.keycloak.federation.ldap;

import java.util.List;
import java.util.Set;

import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPIdentityQuery;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.mappers.LDAPFederationMapper;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;

/**
 * Allow to directly call some operations against LDAPIdentityStore.
 * TODO: Is this class still needed?
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPUtils {

    /**
     * @param ldapProvider
     * @param realm
     * @param user
     * @return newly created LDAPObject with all the attributes, uuid and DN properly set
     */
    public static LDAPObject addUserToLDAP(LDAPFederationProvider ldapProvider, RealmModel realm, UserModel user) {
        LDAPObject ldapObject = new LDAPObject();

        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        LDAPConfig ldapConfig = ldapStore.getConfig();
        ldapObject.setRdnAttributeName(ldapConfig.getRdnLdapAttribute());
        ldapObject.setObjectClasses(ldapConfig.getObjectClasses());

        Set<UserFederationMapperModel> federationMappers = realm.getUserFederationMappers();
        for (UserFederationMapperModel mapperModel : federationMappers) {
            LDAPFederationMapper ldapMapper = ldapProvider.getMapper(mapperModel);
            ldapMapper.onRegisterUserToLDAP(mapperModel, ldapProvider, ldapObject, user, realm);
        }

        LDAPUtils.computeAndSetDn(ldapConfig, ldapObject);
        ldapStore.add(ldapObject);
        return ldapObject;
    }

    /*public static LDAPUser updateUser(LDAPIdentityStore ldapIdentityStore, String username, String firstName, String lastName, String email) {
        LDAPUser ldapUser = getUser(ldapIdentityStore, username);
        ldapUser.setFirstName(firstName);
        ldapUser.setLastName(lastName);
        ldapUser.setEmail(email);
        ldapIdentityStore.update(ldapUser);
        return ldapUser;
    }

    public static void updatePassword(LDAPIdentityStore ldapIdentityStore, UserModel user, String password) {
        LDAPUser ldapUser = convertUserForPasswordUpdate(user);

        ldapIdentityStore.updatePassword(ldapUser, password);
    }

    public static void updatePassword(LDAPIdentityStore ldapIdentityStore, LDAPUser user, String password) {
        ldapIdentityStore.updatePassword(user, password);
    }

    public static boolean validatePassword(LDAPIdentityStore ldapIdentityStore, UserModel user, String password) {
        LDAPUser ldapUser = convertUserForPasswordUpdate(user);

        return ldapIdentityStore.validatePassword(ldapUser, password);
    }

    public static boolean validatePassword(LDAPIdentityStore ldapIdentityStore, LDAPUser user, String password) {
        return ldapIdentityStore.validatePassword(user, password);
    }

    public static LDAPUser getUser(LDAPIdentityStore ldapIdentityStore, String username) {
        return ldapIdentityStore.getUser(username);
    }

    // Put just username and entryDN as these are needed by LDAPIdentityStore for passwordUpdate
    private static LDAPUser convertUserForPasswordUpdate(UserModel kcUser) {
        LDAPUser ldapUser = new LDAPUser(kcUser.getUsername());
        String ldapEntryDN = kcUser.getAttribute(LDAPConstants.LDAP_ENTRY_DN);
        if (ldapEntryDN != null) {
            ldapUser.setEntryDN(ldapEntryDN);
        }
        return ldapUser;
    }


    public static LDAPUser getUserByEmail(LDAPIdentityStore ldapIdentityStore, String email) {
        IdentityQueryBuilder queryBuilder = ldapIdentityStore.createQueryBuilder();
        LDAPIdentityQuery<LDAPUser> query = queryBuilder.createIdentityQuery(LDAPUser.class)
                .where(queryBuilder.equal(LDAPUser.EMAIL, email));
        List<LDAPUser> users = query.getResultList();

        if (users.isEmpty()) {
            return null;
        } else if (users.size() == 1) {
            return users.get(0);
        } else {
            throw new ModelDuplicateException("Error - multiple users found with same email " + email);
        }
    }

    public static boolean removeUser(LDAPIdentityStore ldapIdentityStore, String username) {
        LDAPUser ldapUser = getUser(ldapIdentityStore, username);
        if (ldapUser == null) {
            return false;
        }
        ldapIdentityStore.remove(ldapUser);
        return true;
    }    */

    public static void removeAllUsers(LDAPFederationProvider ldapProvider, RealmModel realm) {
        LDAPIdentityStore ldapStore = ldapProvider.getLdapIdentityStore();
        LDAPIdentityQuery ldapQuery = LDAPUtils.createQueryForUserSearch(ldapProvider, realm);
        List<LDAPObject> allUsers = ldapQuery.getResultList();

        for (LDAPObject ldapUser : allUsers) {
            ldapStore.remove(ldapUser);
        }
    }

    public static LDAPIdentityQuery createQueryForUserSearch(LDAPFederationProvider ldapProvider, RealmModel realm) {
        LDAPIdentityQuery ldapQuery = new LDAPIdentityQuery(ldapProvider);
        LDAPConfig config = ldapProvider.getLdapIdentityStore().getConfig();
        ldapQuery.setSearchScope(config.getSearchScope());
        ldapQuery.addSearchDns(config.getUserDns());
        ldapQuery.addObjectClasses(config.getObjectClasses());

        Set<UserFederationMapperModel> mapperModels = realm.getUserFederationMappers();
        ldapQuery.addMappers(mapperModels);

        return ldapQuery;
    }

    /*
    public static List<LDAPUser> getAllUsers(LDAPIdentityStore ldapIdentityStore) {
        LDAPIdentityQuery<LDAPUser> userQuery = ldapIdentityStore.createQueryBuilder().createIdentityQuery(LDAPUser.class);
        return userQuery.getResultList();
    }

    // Needed for ActiveDirectory updates
    private static String getFullName(String username, String firstName, String lastName) {
        String fullName;
        if (firstName != null && lastName != null) {
            fullName = firstName + " " + lastName;
        } else if (firstName != null && firstName.trim().length() > 0) {
            fullName = firstName;
        } else {
            fullName = lastName;
        }

        // Fallback to loginName
        if (fullName == null || fullName.trim().length() == 0) {
            fullName = username;
        }

        return fullName;
    }   */

    // ldapUser has filled attributes, but doesn't have filled dn
    public static void computeAndSetDn(LDAPConfig config, LDAPObject ldapObject) {
        String rdnLdapAttrName = config.getRdnLdapAttribute();
        String rdnLdapAttrValue = ldapObject.getAttributeAsString(rdnLdapAttrName);
        if (rdnLdapAttrValue == null) {
            throw new ModelException("RDN Attribute [" + rdnLdapAttrName + "] is not filled. Filled attributes: " + ldapObject.getAttributes());
        }

        LDAPDn dn = LDAPDn.fromString(config.getSingleUserDn());
        dn.addToHead(rdnLdapAttrName, rdnLdapAttrValue);
        ldapObject.setDn(dn);
    }

    public static String getUsername(LDAPObject ldapUser, LDAPConfig config) {
        String usernameAttr = config.getUsernameLdapAttribute();
        return ldapUser.getAttributeAsString(usernameAttr);
    }
}
