package org.keycloak.authentication;

import org.keycloak.provider.ProviderFactory;

/**
 * TODO
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ClientAuthenticatorFactory extends ProviderFactory<ClientAuthenticator>, ConfigurableAuthenticatorFactory {
    ClientAuthenticator create();

    /**
     * Is this authenticator configurable globally?
     *
     * @return
     */
    @Override
    boolean isConfigurable();

    /**
     * Is this authenticator configurable per client?
     *
     * @return
     */
    boolean isConfigurablePerClient();

}
