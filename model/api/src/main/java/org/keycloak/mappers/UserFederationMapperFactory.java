package org.keycloak.mappers;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UserFederationMapperFactory extends ProviderFactory<UserFederationMapper>, ConfiguredProvider {
}
