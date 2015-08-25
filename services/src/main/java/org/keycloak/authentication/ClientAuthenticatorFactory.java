package org.keycloak.authentication;

import org.keycloak.provider.ProviderFactory;

/**
 * Factory for creating ClientAuthenticator instances.  This is a singleton and created when Keycloak boots.
 *
 * You must specify a file
 * META-INF/services/org.keycloak.authentication.ClientAuthenticatorFactory in the jar that this class is contained in
 * This file must have the fully qualified class name of all your ClientAuthenticatorFactory classes
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
     * Is this authenticator configurable per client? The configuration will be in "Clients" / "Credentials" tab in admin console
     *
     * @return
     */
    boolean isConfigurablePerClient();

}
