package org.keycloak.authentication.model;

import org.keycloak.Config;
import org.keycloak.authentication.AuthProviderConstants;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExternalModelAuthenticationProviderFactory implements AuthenticationProviderFactory {

    @Override
    public AuthenticationProvider create(ProviderSession providerSession) {
        return new ExternalModelAuthenticationProvider(providerSession);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return AuthProviderConstants.PROVIDER_NAME_EXTERNAL_MODEL;
    }

}
