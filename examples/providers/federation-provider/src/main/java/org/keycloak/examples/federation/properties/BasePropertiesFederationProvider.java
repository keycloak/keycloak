package org.keycloak.examples.federation.properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class BasePropertiesFederationProvider implements UserFederationProvider {
    protected static final Set<String> supportedCredentialTypes = new HashSet<String>();
    protected KeycloakSession session;
    protected Properties properties;
    protected UserFederationProviderModel model;

    public BasePropertiesFederationProvider(KeycloakSession session, UserFederationProviderModel model, Properties properties) {
        this.session = session;
        this.model = model;
        this.properties = properties;
    }

    static
    {
        supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
    }


    public KeycloakSession getSession() {
        return session;
    }

    public Properties getProperties() {
        return properties;
    }

    public UserFederationProviderModel getModel() {
        return model;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        String password = properties.getProperty(username);
        if (password != null) {
            UserModel userModel = session.userStorage().addUser(realm, username);
            userModel.setEnabled(true);
            userModel.setFederationLink(model.getId());
            return userModel;
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    /**
     * We only search for Usernames as that is all that is stored in the properties file.  Not that if the user
     * does exist in the properties file, we only import it if the user hasn't been imported already.
     *
     * @param attributes
     * @param realm
     * @param maxResults
     * @return
     */
    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        String username = attributes.get(USERNAME);
        if (username != null) {
            // make sure user isn't already in storage
            if (session.userStorage().getUserByUsername(username, realm) == null) {
                // user is not already imported, so let's import it until local storage.
                UserModel user = getUserByUsername(realm, username);
                if (user != null) {
                    List<UserModel> list = new ArrayList<UserModel>(1);
                    list.add(user);
                    return list;
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void preRemove(RealmModel realm) {
       // complete  We don't care about the realm being removed
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // complete we dont'care if a role is removed

    }

    /**
     * See if the user is still in the properties file
     *
     * @param local
     * @return
     */
    @Override
    public boolean isValid(UserModel local) {
        return properties.containsKey(local.getUsername());
    }

    /**
     * hardcoded to only return PASSWORD
     *
     * @param user
     * @return
     */
    @Override
    public Set<String> getSupportedCredentialTypes(UserModel user) {
        return supportedCredentialTypes;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                String password = properties.getProperty(user.getUsername());
                if (password == null) return false;
                return password.equals(cred.getValue());
            } else {
                return false; // invalid cred type
            }
        }
        return false;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                String password = properties.getProperty(user.getUsername());
                if (password == null) return false;
                return password.equals(cred.getValue());
            } else {
                return false; // invalid cred type
            }
        }
        return true;
    }

    @Override
    public void close() {

    }
}
