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
public class ClasspathPropertiesFederationProvider extends BasePropertiesFederationProvider {

    public ClasspathPropertiesFederationProvider(KeycloakSession session, UserFederationProviderModel model, Properties properties) {
        super(session, model, properties);
    }

    @Override
    public UserModel proxy(UserModel local) {
        return new ReadonlyUserModelProxy(local);
    }

    @Override
    public boolean synchronizeRegistrations() {
        return false;
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Registration not supported");
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Remove not supported");
    }

}
