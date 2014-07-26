package org.keycloak.authentication.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.authentication.AuthProviderConstants;

/**
 * AbstractModelAuthenticationProvider, which uses current realm to call operations on
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ModelAuthenticationProvider extends AbstractModelAuthenticationProvider {

    public ModelAuthenticationProvider(KeycloakSession keycloakSession) {
        super(keycloakSession);
    }

    @Override
    public String getName() {
        return AuthProviderConstants.PROVIDER_NAME_MODEL;
    }

    @Override
    public List<String> getAvailableOptions() {
        return Collections.EMPTY_LIST;
    }

    @Override
    protected RealmModel getRealm(RealmModel currentRealm, Map<String, String> config) {
        return currentRealm;
    }
}
