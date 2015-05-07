package org.keycloak.federation.ldap;

import org.keycloak.federation.ldap.idm.model.Attribute;
import org.keycloak.federation.ldap.idm.model.LDAPUser;
import org.keycloak.federation.ldap.idm.query.AttributeParameter;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQuery;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQueryBuilder;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserModel;

import java.util.List;

/**
 * Allow to directly call some operations against LDAPIdentityStore.
 * TODO: Is this class still needed?
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPUtils {

    public static QueryParameter MODIFY_DATE = new AttributeParameter("modifyDate");

    public static LDAPUser addUser(LDAPIdentityStore ldapIdentityStore, String username, String firstName, String lastName, String email) {
        if (getUser(ldapIdentityStore, username) != null) {
            throw new ModelDuplicateException("User with same username already exists");
        }
        if (getUserByEmail(ldapIdentityStore, email) != null) {
            throw new ModelDuplicateException("User with same email already exists");
        }

        LDAPUser ldapUser = new LDAPUser(username);
        ldapUser.setFirstName(firstName);
        ldapUser.setLastName(lastName);
        ldapUser.setEmail(email);
        ldapUser.setAttribute(new Attribute<String>("fullName", getFullName(username, firstName, lastName)));
        ldapIdentityStore.add(ldapUser);
        return ldapUser;
    }

    public static LDAPUser updateUser(LDAPIdentityStore ldapIdentityStore, String username, String firstName, String lastName, String email) {
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
        IdentityQuery<LDAPUser> query = queryBuilder.createIdentityQuery(LDAPUser.class)
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
    }

    public static void removeAllUsers(LDAPIdentityStore ldapIdentityStore) {
        List<LDAPUser> allUsers = getAllUsers(ldapIdentityStore);

        for (LDAPUser user : allUsers) {
            ldapIdentityStore.remove(user);
        }
    }

    public static List<LDAPUser> getAllUsers(LDAPIdentityStore ldapIdentityStore) {
        IdentityQuery<LDAPUser> userQuery = ldapIdentityStore.createQueryBuilder().createIdentityQuery(LDAPUser.class);
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
    }
}
