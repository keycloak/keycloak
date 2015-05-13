package org.keycloak.models;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserFederationMapper extends Provider, ProviderFactory<UserFederationMapper>, ConfiguredProvider {

}
