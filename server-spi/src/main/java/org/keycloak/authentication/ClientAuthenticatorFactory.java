package org.keycloak.authentication;

import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.provider.ProviderConfigProperty;
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
     * List of config properties for this client implementation. Those will be shown in admin console in clients credentials tab and can be configured per client.
     * Applicable only if "isConfigurablePerClient" is true
     *
     * @return
     */
    List<ProviderConfigProperty> getConfigPropertiesPerClient();

    /**
     * Get configuration, which needs to be used for adapter ( keycloak.json ) of particular client. Some implementations
     * may return just template and user needs to edit the values according to his environment (For example fill the location of keystore file)
     *
     * @return
     */
    Map<String, Object> getAdapterConfiguration(ClientModel client);

}
