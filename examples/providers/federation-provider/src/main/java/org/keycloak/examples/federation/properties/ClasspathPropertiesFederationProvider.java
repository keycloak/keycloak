package org.keycloak.examples.federation.properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;

import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClasspathPropertiesFederationProvider extends BasePropertiesFederationProvider {

    public ClasspathPropertiesFederationProvider(KeycloakSession session, UserFederationProviderModel model, Properties properties) {
        super(session, model, properties);
    }

    /**
     * Keycloak will call this method if it finds an imported UserModel.  Here we proxy the UserModel with
     * a Readonly proxy which will barf if password is updated.
     *
     * @param local
     * @return
     */
    @Override
    public UserModel proxy(UserModel local) {
        return new ReadonlyUserModelProxy(local);
    }

    /**
     * The properties file is readonly so don't suppport registration.
     *
     * @return
     */
    @Override
    public boolean synchronizeRegistrations() {
        return false;
    }

    /**
     * The properties file is readonly so don't suppport registration.
     *
     * @return
     */
    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Registration not supported");
    }

    /**
     * The properties file is readonly so don't removing a user
     *
     * @return
     */
    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        throw new IllegalStateException("Remove not supported");
    }

}
