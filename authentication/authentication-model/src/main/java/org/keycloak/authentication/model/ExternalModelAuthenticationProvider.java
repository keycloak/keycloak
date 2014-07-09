package org.keycloak.authentication.model;

import org.keycloak.authentication.AuthProviderConstants;
import org.keycloak.authentication.AuthenticationProviderException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.RealmModel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * AbstractModelAuthenticationProvider, which delegates authentication operations to different (external) realm
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExternalModelAuthenticationProvider extends AbstractModelAuthenticationProvider {

    private ModelProvider model;

    public ExternalModelAuthenticationProvider(KeycloakSession session) {
        this.model = session.model();
    }

    @Override
    public String getName() {
        return AuthProviderConstants.PROVIDER_NAME_EXTERNAL_MODEL;
    }

    @Override
    public List<String> getAvailableOptions() {
        return Arrays.asList(AuthProviderConstants.EXTERNAL_REALM_ID);
    }

    @Override
    public RealmModel getRealm(RealmModel currentRealm, Map<String, String> configuration) throws AuthenticationProviderException {
        String realmId = configuration.get(AuthProviderConstants.EXTERNAL_REALM_ID);
        if (realmId == null) {
            throw new AuthenticationProviderException("Option '" + AuthProviderConstants.EXTERNAL_REALM_ID + "' not specified in configuration");
        }

        RealmModel realm = model.getRealm(realmId);
        if (realm == null) {
            throw new AuthenticationProviderException("Realm with id '" + realmId + "' doesn't exists");
        }
        return realm;
    }
}
