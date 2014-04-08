package org.keycloak.authentication.model;

import org.keycloak.authentication.AuthProviderConstants;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderFactory;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ModelAuthenticationProviderFactory implements AuthenticationProviderFactory {

    @Override
    public AuthenticationProvider create(ProviderSession providerSession) {
        return new ModelAuthenticationProvider();
    }

    @Override
    public void init() {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return AuthProviderConstants.PROVIDER_NAME_MODEL;
    }

    @Override
    public boolean lazyLoad() {
        return false;
    }
}
