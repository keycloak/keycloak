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

    public static Set<String> getSupportedCredentialTypes() {
        return supportedCredentialTypes;
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
            userModel.updateCredential(UserCredentialModel.password(password));
            return userModel;
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return null;
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        if (attributes.containsKey(USERNAME)) {
            UserModel user = getUserByUsername(realm, attributes.get(USERNAME));
            if (user != null) {
                List<UserModel> list = new ArrayList<UserModel>(1);
                list.add(user);
                return list;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public boolean isValid(UserModel local) {
        return properties.containsKey(local.getUsername());
    }

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
