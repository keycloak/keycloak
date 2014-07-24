package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPFederationProvider implements UserFederationProvider {
    private static final Logger logger = Logger.getLogger(LDAPFederationProvider.class);

    protected KeycloakSession session;
    protected UserFederationProviderModel model;
    protected PartitionManager partitionManager;

    protected static final Set<String> supportedCredentialTypes = new HashSet<String>();

    static
    {
        supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
    }

    public LDAPFederationProvider(KeycloakSession session, UserFederationProviderModel model, PartitionManager partitionManager) {
        this.session = session;
        this.model = model;
        this.partitionManager = partitionManager;
    }

    private ModelException convertIDMException(IdentityManagementException ie) {
        Throwable realCause = ie;
        while (realCause.getCause() != null) {
            realCause = realCause.getCause();
        }

        // Use the message from the realCause
        return new ModelException(realCause.getMessage(), ie);
    }

    public KeycloakSession getSession() {
        return session;
    }

    public UserFederationProviderModel getModel() {
        return model;
    }

    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    @Override
    public UserModel proxy(UserModel local) {
        // todo
        return new LDAPUserModelDelegate(local, this);
    }

    @Override
    public Set<String> getSupportedCredentialTypes() {
        return supportedCredentialTypes;
    }

    @Override
    public UserModel addUser(RealmModel realm, UserModel user) {
        IdentityManager identityManager = getIdentityManager();

        try {
            User picketlinkUser = new User(user.getUsername());
            picketlinkUser.setFirstName(user.getFirstName());
            picketlinkUser.setLastName(user.getLastName());
            picketlinkUser.setEmail(user.getEmail());
            identityManager.add(picketlinkUser);
            return proxy(user);
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        IdentityManager identityManager = getIdentityManager();

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, user.getUsername());
            if (picketlinkUser == null) {
                return false;
            }
            identityManager.remove(picketlinkUser);
            return true;
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    @Override
    public String getAdminPage() {
        return null;
    }

    @Override
    public Class getAdminClass() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles) {
        throw new IllegalAccessError("Not allowed to call this");
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        throw new IllegalAccessError("Not allowed to call this");
    }

    @Override
    public boolean removeUser(RealmModel realm, String name) {
        throw new IllegalAccessError("Not allowed to call this");
    }

    @Override
    public void addSocialLink(RealmModel realm, UserModel user, SocialLinkModel socialLink) {
        session.userStorage().addSocialLink(realm, user, socialLink);
    }

    @Override
    public boolean removeSocialLink(RealmModel realm, UserModel user, String socialProvider) {
        return session.userStorage().removeSocialLink(realm, user, socialProvider);
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        return null;
    }


    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        IdentityManager identityManager = getIdentityManager();

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, username);
            if (picketlinkUser == null) {
                return null;
            }

            return importUserFromPicketlink(realm, picketlinkUser);
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    public IdentityManager getIdentityManager() {
        return partitionManager.createIdentityManager();
    }

    protected UserModel importUserFromPicketlink(RealmModel realm, User picketlinkUser) {
        String email = (picketlinkUser.getEmail() != null && picketlinkUser.getEmail().trim().length() > 0) ? picketlinkUser.getEmail() : null;
        UserModel imported = session.userStorage().addUser(realm, picketlinkUser.getLoginName());
        imported.setEnabled(true);
        imported.setEmail(email);
        imported.setFirstName(picketlinkUser.getFirstName());
        imported.setLastName(picketlinkUser.getLastName());
        imported.setFederationLink(model.getId());
        return proxy(imported);
    }

    protected User queryByEmail(IdentityManager identityManager, String email) throws IdentityManagementException {
        List<User> agents = identityManager.createIdentityQuery(User.class)
                .setParameter(User.EMAIL, email).getResultList();

        if (agents.isEmpty()) {
            return null;
        } else if (agents.size() == 1) {
            return agents.get(0);
        } else {
            throw new IdentityManagementException("Error - multiple Agent objects found with same login name");
        }
    }


    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        IdentityManager identityManager = getIdentityManager();

        try {
            User picketlinkUser = queryByEmail(identityManager, email);
            if (picketlinkUser == null) {
                return null;
            }

            return importUserFromPicketlink(realm, picketlinkUser);
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink, RealmModel realm) {
        return null;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return -1;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SocialLinkModel getSocialLink(UserModel user, String socialProvider, RealmModel realm) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void preRemove(RealmModel realm) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean validPassword(String username, String password) {
        IdentityManager identityManager = getIdentityManager();

        try {
            UsernamePasswordCredentials credential = new UsernamePasswordCredentials();
            credential.setUsername(username);
            credential.setPassword(new Password(password.toCharArray()));
            identityManager.validateCredentials(credential);
            if (credential.getStatus() == Credentials.Status.VALID) {
                return true;
            } else {
                return false;
            }
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }


    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return validPassword(user.getUsername(), cred.getValue());
            } else {
                return false; // invalid cred type
            }
        }
        return true;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return validPassword(user.getUsername(), cred.getValue());
            } else {
                return false; // invalid cred type
            }
        }
        return true;
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
