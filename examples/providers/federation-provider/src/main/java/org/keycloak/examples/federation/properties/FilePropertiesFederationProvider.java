package org.keycloak.examples.federation.properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;

import java.io.FileOutputStream;
import java.io.IOException;
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
public class FilePropertiesFederationProvider extends BasePropertiesFederationProvider {

    public FilePropertiesFederationProvider(KeycloakSession session, Properties properties, UserFederationProviderModel model) {
        super(session, model, properties);
    }

    @Override
    public UserModel proxy(UserModel local) {
        return new WritableUserModelProxy(local, this);
    }

    @Override
    public boolean synchronizeRegistrations() {
        return true;
    }

    public void save() {
        String path = getModel().getConfig().get("path");
        try {
            FileOutputStream fos = new FileOutputStream(path);
            properties.store(fos, "");
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        synchronized (properties) {
            properties.setProperty(user.getUsername(), "");
            save();
        }
        return proxy(user);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        synchronized (properties) {
            if (properties.remove(user.getUsername()) == null) return false;
            save();
            return true;
        }
    }



}
