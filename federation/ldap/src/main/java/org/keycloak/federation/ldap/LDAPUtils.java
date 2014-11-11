package org.keycloak.federation.ldap;

import org.keycloak.models.ModelDuplicateException;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.query.AttributeParameter;
import org.picketlink.idm.query.QueryParameter;

import java.util.List;

/**
 * Allow to directly call some operations against Picketlink IDM PartitionManager (hence LDAP).
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPUtils {

    public static QueryParameter MODIFY_DATE = new AttributeParameter("modifyDate");

    public static User addUser(PartitionManager partitionManager, String username, String firstName, String lastName, String email) {
        IdentityManager identityManager = getIdentityManager(partitionManager);

        if (BasicModel.getUser(identityManager, username) != null) {
            throw new ModelDuplicateException("User with same username already exists");
        }
        if (getUserByEmail(identityManager, email) != null) {
            throw new ModelDuplicateException("User with same email already exists");
        }

        User picketlinkUser = new User(username);
        picketlinkUser.setFirstName(firstName);
        picketlinkUser.setLastName(lastName);
        picketlinkUser.setEmail(email);
        picketlinkUser.setAttribute(new Attribute("fullName", getFullName(username, firstName, lastName)));
        identityManager.add(picketlinkUser);
        return picketlinkUser;
    }

    public static User updateUser(PartitionManager partitionManager, String username, String firstName, String lastName, String email) {
        IdentityManager idmManager = getIdentityManager(partitionManager);
        User picketlinkUser = BasicModel.getUser(idmManager, username);
        picketlinkUser.setFirstName(firstName);
        picketlinkUser.setLastName(lastName);
        picketlinkUser.setEmail(email);
        idmManager.update(picketlinkUser);
        return picketlinkUser;
    }

    public static void updatePassword(PartitionManager partitionManager, User picketlinkUser, String password) {
        IdentityManager idmManager = getIdentityManager(partitionManager);
        idmManager.updateCredential(picketlinkUser, new Password(password.toCharArray()));
    }

    public static boolean validatePassword(PartitionManager partitionManager, String username, String password) {
        IdentityManager idmManager = getIdentityManager(partitionManager);

        UsernamePasswordCredentials credential = new UsernamePasswordCredentials();
        credential.setUsername(username);
        credential.setPassword(new Password(password.toCharArray()));
        idmManager.validateCredentials(credential);
        if (credential.getStatus() == Credentials.Status.VALID) {
            return true;
        } else {
            return false;
        }
    }

    public static User getUser(PartitionManager partitionManager, String username) {
        IdentityManager idmManager = getIdentityManager(partitionManager);
        return BasicModel.getUser(idmManager, username);
    }


    public static User getUserByEmail(IdentityManager idmManager, String email) throws IdentityManagementException {
        List<User> agents = idmManager.createIdentityQuery(User.class)
                .setParameter(User.EMAIL, email).getResultList();

        if (agents.isEmpty()) {
            return null;
        } else if (agents.size() == 1) {
            return agents.get(0);
        } else {
            throw new IdentityManagementException("Error - multiple users found with same email");
        }
    }

    public static boolean removeUser(PartitionManager partitionManager, String username) {
        IdentityManager idmManager = getIdentityManager(partitionManager);
        User picketlinkUser = BasicModel.getUser(idmManager, username);
        if (picketlinkUser == null) {
            return false;
        }
        idmManager.remove(picketlinkUser);
        return true;
    }

    public static void removeAllUsers(PartitionManager partitionManager) {
        IdentityManager idmManager = getIdentityManager(partitionManager);
        List<User> users = idmManager.createIdentityQuery(User.class).getResultList();

        for (User user : users) {
            idmManager.remove(user);
        }
    }

    public static List<User> getAllUsers(PartitionManager partitionManager) {
        IdentityManager idmManager = getIdentityManager(partitionManager);
        return idmManager.createIdentityQuery(User.class).getResultList();
    }

    private static IdentityManager getIdentityManager(PartitionManager partitionManager) {
        return partitionManager.createIdentityManager();
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
