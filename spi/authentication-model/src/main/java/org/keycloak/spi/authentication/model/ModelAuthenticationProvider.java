package org.keycloak.spi.authentication.model;

import java.util.Map;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.spi.authentication.AuthProviderConstants;
import org.keycloak.spi.authentication.AuthenticatedUser;

/**
 * AbstractModelAuthenticationProvider, which uses current realm to call operations on
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ModelAuthenticationProvider extends AbstractModelAuthenticationProvider {

    @Override
    public String getName() {
        return AuthProviderConstants.PROVIDER_NAME_MODEL;
    }

    @Override
    protected RealmModel getRealm(RealmModel currentRealm, Map<String, String> config) {
        return currentRealm;
    }

    @Override
    protected AuthenticatedUser createAuthenticatedUserInstance(UserModel user) {
        // We don't want AuthenticatedUser instance. Auto-registration won't never happen with this provider
        return null;
    }
}
