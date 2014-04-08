package org.keycloak.authentication.picketlink;

import org.keycloak.authentication.AuthProviderConstants;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderFactory;
import org.keycloak.picketlink.IdentityManagerProvider;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PicketlinkAuthenticationProviderFactory implements AuthenticationProviderFactory {

    @Override
    public AuthenticationProvider create(ProviderSession providerSession) {
        return new PicketlinkAuthenticationProvider(providerSession.getProvider(IdentityManagerProvider.class));
    }

    @Override
    public void init() {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return AuthProviderConstants.PROVIDER_NAME_PICKETLINK;
    }

    @Override
    public boolean lazyLoad() {
        return false;
    }
}
